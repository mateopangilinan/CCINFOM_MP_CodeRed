package model.service;

import config.Database;

import model.dao.*;
import model.entity.*;

import utilities.Validation;

import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * CycleService.java
 *
 * Validates and authorizes cycle operations, then calls the cycle DAO. After adding, editing or
 * deleting a cycle, it recalculates every affected length and both user averages in the same
 * transaction.
 */
public final class CycleService {
    private final Database db;
    private final UserDao users;
    private final CycleDao cycles;
    private final Session session;
    private final Clock clock;

    public CycleService(Database db, UserDao users, CycleDao cycles, Session session, Clock clock) {
        this.db = db;
        this.users = users;
        this.cycles = cycles;
        this.session = session;
        this.clock = clock;
    }

    // Validate the filters and current access before asking the DAO for matching records.
    public List<Cycle> list(Integer owner, Integer year, Integer month) {
        LocalDate from = Filters.month(year, month);
        return Transactions.run(
                db,
                new Transactions.Work<List<Cycle>>() {
                    @Override
                    public List<Cycle> run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Filters.owner(owner, scope);
                        LocalDate until = null;
                        if (from != null) {
                            until = from.plusMonths(1);
                        }
                        return cycles.list(c, scope, owner, from, until);
                    }
                });
    }

    // Check current access and return the requested record, or report it as unavailable.
    public Cycle find(int id) {
        return Transactions.run(
                db,
                new Transactions.Work<Cycle>() {
                    @Override
                    public Cycle run(Connection c) throws SQLException {
                        return Access.found(cycles.find(c, id, Access.scope(c, users, session)));
                    }
                });
    }

    // Validate input and ownership, then save the new record inside one transaction.
    public int add(int owner, LocalDate start, LocalDate end, String notes) {
        String n = Validation.notes(notes);
        return Transactions.run(
                db,
                new Transactions.Work<Integer>() {
                    @Override
                    public Integer run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Access.lockOwner(c, users, owner, scope);
                        // Insert first, then update all affected lengths and averages before this
                        // transaction commits.
                        int id = cycles.insert(c, owner, start, end, n);
                        recalculate(c, owner, scope);
                        return id;
                    }
                });
    }

    // Check the existing record and its owner before saving the allowed changes.
    public void update(int id, LocalDate start, LocalDate end, String notes) {
        String n = Validation.notes(notes);
        Transactions.run(
                db,
                new Transactions.Work<Object>() {
                    @Override
                    public Object run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Cycle old = Access.found(cycles.find(c, id, scope));
                        Access.lockOwner(c, users, old.userId(), scope);
                        Access.found(cycles.find(c, id, scope));
                        // An edited date may change neighboring cycle lengths, so rebuild the
                        // owner's whole history next.
                        cycles.update(c, id, scope, start, end, n);
                        recalculate(c, old.userId(), scope);
                        return null;
                    }
                });
    }

    // Check access to the saved record before deleting it inside a transaction.
    public void delete(int id) {
        Transactions.run(
                db,
                new Transactions.Work<Object>() {
                    @Override
                    public Object run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Cycle old = Access.found(cycles.find(c, id, scope));
                        Access.lockOwner(c, users, old.userId(), scope);
                        // Deleting a start changes the gap between its neighbors; recalculate
                        // before committing.
                        cycles.delete(c, id, scope);
                        recalculate(c, old.userId(), scope);
                        return null;
                    }
                });
    }

    private void recalculate(Connection c, int owner, int scope) throws SQLException {
        // Rebuild sorted history so backdated changes also correct later lengths and averages.
        // The caller keeps these derived updates in the same transaction as the cycle edit.
        CycleRules.History history =
                CycleRules.calculate(cycles.list(c, scope, owner, null, null), clock);
        for (Cycle v : history.cycles()) cycles.length(c, v.id(), owner, v.length());
        users.averages(c, owner, history.cycleAverage(), history.periodAverage());
    }
}
