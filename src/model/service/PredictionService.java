package model.service;

import config.Database;

import model.dao.*;
import model.entity.*;
import model.service.PredictionService.Generated;

import utilities.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

/**
 * PredictionService.java
 *
 * Handles manual prediction records and generation of saved estimates from cycle history. It
 * validates dates, checks ownership and coordinates duplicate checks and saving within
 * transactions.
 */
public final class PredictionService {
    private final Database db;
    private final UserDao users;
    private final CycleDao cycles;
    private final PredictionDao predictions;
    private final Session session;
    private final Clock clock;

    public PredictionService(
            Database db,
            UserDao users,
            CycleDao cycles,
            PredictionDao predictions,
            Session session,
            Clock clock) {
        this.db = db;
        this.users = users;
        this.cycles = cycles;
        this.predictions = predictions;
        this.session = session;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // Validate the filters and current access before asking the DAO for matching records.
    public List<Prediction> list(Integer owner, Integer year, Integer month) {
        LocalDate from = Filters.month(year, month);
        return Transactions.run(
                db,
                new Transactions.Work<List<Prediction>>() {
                    @Override
                    public List<Prediction> run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Filters.owner(owner, scope);
                        LocalDate until = null;
                        if (from != null) {
                            until = from.plusMonths(1);
                        }
                        return predictions.list(c, scope, owner, from, until);
                    }
                });
    }

    // Check current access and return the requested record, or report it as unavailable.
    public Prediction find(int id) {
        return Transactions.run(
                db,
                new Transactions.Work<Prediction>() {
                    @Override
                    public Prediction run(Connection c) throws SQLException {
                        return Access.found(
                                predictions.find(c, id, Access.scope(c, users, session)));
                    }
                });
    }

    private void validate(LocalDate date, LocalDate start, LocalDate end, int range) {
        Validation.require(
                date != null && !date.isAfter(today()),
                "Prediction date must be today or earlier.");
        Validation.require(
                start != null && start.isAfter(date),
                "Predicted start must be after the prediction date.");
        Validation.require(
                end != null && !end.isBefore(start),
                "Predicted end must be on or after its start.");
        Validation.require(range >= 0, "Range must be zero or more days.");
    }

    // Validate input and ownership, then save the new record inside one transaction.
    public int add(int owner, LocalDate date, LocalDate start, LocalDate end, int range) {
        validate(date, start, end, range);
        return Transactions.run(
                db,
                new Transactions.Work<Integer>() {
                    @Override
                    public Integer run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Access.lockOwner(c, users, owner, scope);
                        return predictions.insert(
                                c,
                                new Prediction(
                                        0, owner, date, start, end, "Manual estimate", range));
                    }
                });
    }

    // Check the existing record and its owner before saving the allowed changes.
    public void update(int id, LocalDate start, LocalDate end, int range) {
        Transactions.run(
                db,
                new Transactions.Work<Object>() {
                    @Override
                    public Object run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Prediction old = Access.found(predictions.find(c, id, scope));
                        Access.lockOwner(c, users, old.userId(), scope);
                        old = Access.found(predictions.find(c, id, scope));
                        validate(old.date(), start, end, range);
                        predictions.update(c, id, scope, start, end, range);
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
                        Prediction old = Access.found(predictions.find(c, id, scope));
                        Access.lockOwner(c, users, old.userId(), scope);
                        predictions.delete(c, id, scope);
                        return null;
                    }
                });
    }

    public record Generated(Prediction prediction, boolean saved) {}

    // Calculate and save an estimate using a locked copy of this owner's cycle history.
    public Generated generate(int owner, boolean saveDuplicate) {
        return Transactions.run(
                db,
                new Transactions.Work<Generated>() {
                    @Override
                    public Generated run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Access.lockOwner(c, users, owner, scope);
                        Prediction estimate =
                                PredictionRules.estimate(
                                        owner, cycles.list(c, scope, owner, null, null), today());
                        Prediction existing = predictions.identical(c, estimate);
                        if (existing != null && !saveDuplicate)
                            return new Generated(existing, false);
                        int id = predictions.insert(c, estimate);
                        return new Generated(predictions.find(c, id, scope), true);
                    }
                });
    }
}
