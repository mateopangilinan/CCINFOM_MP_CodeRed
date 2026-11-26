package model.dao;

import model.entity.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * DailyLogDao.java
 *
 * Contains SQL for daily check-in records, their filters and linked symptom counts. Scoped
 * queries restrict records to the owner allowed by the service.
 */
public final class DailyLogDao {
    // Convert one database row into an entity, preserving optional values as null.
    private DailyLog read(ResultSet r) throws SQLException {
        return new DailyLog(
                r.getInt("log_id"),
                r.getInt("user_id"),
                Jdbc.date(r, "log_date"),
                r.getString("flow_level"),
                r.getString("mood"),
                r.getInt("pain_level"),
                r.getString("notes"));
    }

    // Look up one ID and apply the ownership condition in the same SQL query.
    public DailyLog find(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT * FROM daily_logs WHERE log_id=? AND (?=0 OR user_id=?)",
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
    public List<DailyLog> list(Connection c, int scope, Integer owner, String flow)
            throws SQLException {
        List<DailyLog> rows = new ArrayList<>();
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT * FROM daily_logs WHERE (?=0 OR user_id=?) AND (? IS NULL"
                                        + " OR user_id=?) AND (? IS NULL OR flow_level=?) ORDER BY"
                                        + " log_date,log_id",
                                scope,
                                scope,
                                owner,
                                owner,
                                flow,
                                flow);
                var r = p.executeQuery()) {
            while (r.next()) rows.add(read(r));
        }
        return rows;
    }

    // Insert the supplied values using the service's Connection and return the new ID.
    public int insert(
            Connection c,
            int owner,
            LocalDate date,
            String flow,
            String mood,
            int pain,
            String notes)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "INSERT INTO daily_logs(user_id,log_date,flow_level,mood,pain_level,notes)"
                                + " VALUES(?,?,?,?,?,?)",
                        owner,
                        date,
                        flow,
                        mood,
                        pain,
                        notes)) {
            return Jdbc.insert(p);
        }
    }

    // Update the permitted row using bound values; the service controls the transaction.
    public void update(
            Connection c,
            int id,
            int scope,
            LocalDate date,
            String flow,
            String mood,
            int pain,
            String notes)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "UPDATE daily_logs SET log_date=?,flow_level=?,mood=?,pain_level=?,notes=?"
                                + " WHERE log_id=? AND (?=0 OR user_id=?)",
                        date,
                        flow,
                        mood,
                        pain,
                        notes,
                        id,
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
                        "DELETE FROM daily_logs WHERE log_id=? AND (?=0 OR user_id=?)",
                        id,
                        scope,
                        scope)) {
            Jdbc.changed(p);
        }
    }

    // Count linked symptoms so the delete confirmation can explain what will also be removed.
    public int symptomCount(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT COUNT(s.symptom_id) FROM daily_logs l LEFT JOIN symptoms s"
                                        + " ON s.log_id=l.log_id WHERE l.log_id=? AND (?=0 OR"
                                        + " l.user_id=?)",
                                id,
                                scope,
                                scope);
                var r = p.executeQuery()) {
            r.next();
            return r.getInt(1);
        }
    }
}
