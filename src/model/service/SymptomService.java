package model.service;

import config.Database;

import model.dao.*;
import model.entity.*;

import utilities.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * SymptomService.java
 *
 * Validates symptom values and checks access to both symptoms and their linked daily logs. It
 * keeps symptom changes within the owner permitted by the signed-in account.
 */
public final class SymptomService {
    private final Database db;
    private final UserDao users;
    private final DailyLogDao logs;
    private final SymptomDao symptoms;
    private final Session session;

    public SymptomService(
            Database db, UserDao users, DailyLogDao logs, SymptomDao symptoms, Session session) {
        this.db = db;
        this.users = users;
        this.logs = logs;
        this.symptoms = symptoms;
        this.session = session;
    }

    // Validate the filters and current access before asking the DAO for matching records.
    public List<Symptom> list(Integer owner, String type) {
        if (type != null) Validation.option(type, Choices.SYMPTOMS);
        return Transactions.run(
                db,
                new Transactions.Work<List<Symptom>>() {
                    @Override
                    public List<Symptom> run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Filters.owner(owner, scope);
                        return symptoms.list(c, scope, owner, type);
                    }
                });
    }

    // Check current access and return the requested record, or report it as unavailable.
    public Symptom find(int id) {
        return Transactions.run(
                db,
                new Transactions.Work<Symptom>() {
                    @Override
                    public Symptom run(Connection c) throws SQLException {
                        return Access.found(symptoms.find(c, id, Access.scope(c, users, session)));
                    }
                });
    }

    private void validate(String type, int severity) {
        Validation.option(type, Choices.SYMPTOMS);
        Validation.require(severity >= 1 && severity <= 5, "Severity must be 1–5.");
    }

    // Validate input and ownership, then save the new record inside one transaction.
    public int add(int log, String type, int severity) {
        validate(type, severity);
        return Transactions.run(
                db,
                new Transactions.Work<Integer>() {
                    @Override
                    public Integer run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        DailyLog target = Access.found(logs.find(c, log, scope));
                        Access.lockOwner(c, users, target.userId(), scope);
                        return symptoms.insert(c, log, scope, type, severity);
                    }
                });
    }

    // Check the existing record and its owner before saving the allowed changes.
    public void update(int id, int log, String type, int severity) {
        validate(type, severity);
        Transactions.run(
                db,
                new Transactions.Work<Object>() {
                    @Override
                    public Object run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Symptom old = Access.found(symptoms.find(c, id, scope));
                        DailyLog target = Access.found(logs.find(c, log, scope));
                        // Consistent ascending owner order also supports an admin correcting a log
                        // across users.
                        Access.lockOwner(c, users, Math.min(old.userId(), target.userId()), scope);
                        Access.lockOwner(c, users, Math.max(old.userId(), target.userId()), scope);
                        symptoms.update(c, id, scope, log, type, severity);
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
                        Symptom old = Access.found(symptoms.find(c, id, scope));
                        Access.lockOwner(c, users, old.userId(), scope);
                        symptoms.delete(c, id, scope);
                        return null;
                    }
                });
    }
}
