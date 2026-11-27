package model.service;

import config.Database;

import model.dao.*;
import model.entity.*;

import utilities.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

/**
 * DailyLogService.java
 *
 * Validates and authorizes daily check-in operations before calling the log DAO. It also
 * supplies related-record counts needed to explain the effect of deleting a log.
 */
public final class DailyLogService {
    private final Database db;
    private final UserDao users;
    private final DailyLogDao logs;
    private final Session session;
    private final Clock clock;

    public DailyLogService(
            Database db, UserDao users, DailyLogDao logs, Session session, Clock clock) {
        this.db = db;
        this.users = users;
        this.logs = logs;
        this.session = session;
        this.clock = clock;
    }

    // Validate the filters and current access before asking the DAO for matching records.
    public List<DailyLog> list(Integer owner, String flow) {
        if (flow != null) Validation.option(flow, Choices.FLOWS);
        return Transactions.run(
                db,
                new Transactions.Work<List<DailyLog>>() {
                    @Override
                    public List<DailyLog> run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Filters.owner(owner, scope);
                        return logs.list(c, scope, owner, flow);
                    }
                });
    }

    // Check current access and return the requested record, or report it as unavailable.
    public DailyLog find(int id) {
        return Transactions.run(
                db,
                new Transactions.Work<DailyLog>() {
                    @Override
                    public DailyLog run(Connection c) throws SQLException {
                        return Access.found(logs.find(c, id, Access.scope(c, users, session)));
                    }
                });
    }

    private void validate(LocalDate date, String flow, String mood, int pain) {
        Validation.actual(date, clock);
        Validation.option(flow, Choices.FLOWS);
        Validation.option(mood, Choices.MOODS);
        Validation.require(pain >= 0 && pain <= 10, "Pain must be 0–10.");
    }

    // Validate input and ownership, then save the new record inside one transaction.
    public int add(int owner, LocalDate date, String flow, String mood, int pain, String notes) {
        validate(date, flow, mood, pain);
        String n = Validation.notes(notes);
        return Transactions.run(
                db,
                new Transactions.Work<Integer>() {
                    @Override
                    public Integer run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Access.lockOwner(c, users, owner, scope);
                        return logs.insert(c, owner, date, flow, mood, pain, n);
                    }
                });
    }

    // Check the existing record and its owner before saving the allowed changes.
    public void update(int id, LocalDate date, String flow, String mood, int pain, String notes) {
        validate(date, flow, mood, pain);
        String n = Validation.notes(notes);
        Transactions.run(
                db,
                new Transactions.Work<Object>() {
                    @Override
                    public Object run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        DailyLog old = Access.found(logs.find(c, id, scope));
                        Access.lockOwner(c, users, old.userId(), scope);
                        logs.update(c, id, scope, date, flow, mood, pain, n);
                        return null;
                    }
                });
    }

    // Check access before counting the records that an account deletion would remove.
    public int affected(int id) {
        return Transactions.run(
                db,
                new Transactions.Work<Integer>() {
                    @Override
                    public Integer run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Access.found(logs.find(c, id, scope));
                        return logs.symptomCount(c, id, scope);
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
                        DailyLog old = Access.found(logs.find(c, id, scope));
                        Access.lockOwner(c, users, old.userId(), scope);
                        logs.delete(c, id, scope);
                        return null;
                    }
                });
    }
}
