package model.dao;

import model.entity.*;

import java.sql.*;
import java.util.*;

/**
 * SymptomDao.java
 *
 * Contains SQL for symptom records and their filters. Queries join symptoms to daily logs so
 * ownership restrictions are enforced through the log's user ID.
 */
public final class SymptomDao {
    // Convert one database row into an entity, preserving optional values as null.
    private Symptom read(ResultSet r) throws SQLException {
        return new Symptom(
                r.getInt("symptom_id"),
                r.getInt("log_id"),
                r.getInt("user_id"),
                r.getString("symptom_type"),
                r.getInt("severity"));
    }

    // Look up one ID and apply the ownership condition in the same SQL query.
    public Symptom find(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT s.*,l.user_id FROM symptoms s JOIN daily_logs l ON"
                                        + " l.log_id=s.log_id WHERE s.symptom_id=? AND (?=0 OR"
                                        + " l.user_id=?)",
                                id,
                                scope,
                                scope);
                var r = p.executeQuery()) {
            if (r.next()) {
                return read(r);
            } else {
                return null;
            }
        }
    }

    // Return only the rows allowed by scope and the selected filters.
    public List<Symptom> list(Connection c, int scope, Integer owner, String type)
            throws SQLException {
        List<Symptom> rows = new ArrayList<>();
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT s.*,l.user_id FROM symptoms s JOIN daily_logs l ON"
                                    + " l.log_id=s.log_id WHERE (?=0 OR l.user_id=?) AND (? IS NULL"
                                    + " OR l.user_id=?) AND (? IS NULL OR s.symptom_type=?) ORDER"
                                    + " BY s.symptom_id",
                                scope,
                                scope,
                                owner,
                                owner,
                                type,
                                type);
                var r = p.executeQuery()) {
            while (r.next()) rows.add(read(r));
        }
        return rows;
    }

    // Insert the supplied values using the service's Connection and return the new ID.
    public int insert(Connection c, int log, int scope, String type, int severity)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "INSERT INTO symptoms(log_id,symptom_type,severity) SELECT log_id,?,? FROM"
                                + " daily_logs WHERE log_id=? AND (?=0 OR user_id=?)",
                        type,
                        severity,
                        log,
                        scope,
                        scope)) {
            return Jdbc.insert(p);
        }
    }

    // Update the permitted row using bound values; the service controls the transaction.
    public void update(Connection c, int id, int scope, int log, String type, int severity)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "UPDATE symptoms s JOIN daily_logs old_log ON old_log.log_id=s.log_id JOIN"
                            + " daily_logs new_log ON new_log.log_id=? SET"
                            + " s.log_id=?,s.symptom_type=?,s.severity=? WHERE s.symptom_id=? AND"
                            + " (?=0 OR (old_log.user_id=? AND new_log.user_id=?))",
                        log,
                        log,
                        type,
                        severity,
                        id,
                        scope,
                        scope,
                        scope)) {
            Jdbc.changed(p);
        }
    }

    // Delete only a row allowed by scope; related rows follow the schema's foreign-key rules.
    public void delete(Connection c, int id, int scope) throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "DELETE s FROM symptoms s JOIN daily_logs l ON l.log_id=s.log_id WHERE"
                                + " s.symptom_id=? AND (?=0 OR l.user_id=?)",
                        id,
                        scope,
                        scope)) {
            Jdbc.changed(p);
        }
    }
}
