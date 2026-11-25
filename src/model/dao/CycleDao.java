package model.dao;

import model.entity.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * CycleDao.java
 *
 * Contains SQL for reading, filtering and changing cycle records and saving recalculated
 * lengths. Services supply the connection and access scope and decide when to commit.
 */
public final class CycleDao {
    // Convert one database row into an entity, preserving optional values as null.
    private Cycle read(ResultSet r) throws SQLException {
        return new Cycle(
                r.getInt("cycle_id"),
                r.getInt("user_id"),
                Jdbc.date(r, "start_date"),
                Jdbc.date(r, "period_end_date"),
                Jdbc.integer(r, "cycle_length"),
                r.getString("notes"));
    }

    // Look up one ID and apply the ownership condition in the same SQL query.
    public Cycle find(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT * FROM cycles WHERE cycle_id=? AND (?=0 OR user_id=?)",
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
    public List<Cycle> list(Connection c, int scope, Integer owner, LocalDate from, LocalDate until)
            throws SQLException {
        List<Cycle> rows = new ArrayList<>();
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT * FROM cycles WHERE (?=0 OR user_id=?) AND (? IS NULL OR"
                                    + " user_id=?) AND (? IS NULL OR start_date>=?) AND (? IS NULL"
                                    + " OR start_date<?) ORDER BY start_date,cycle_id",
                                scope,
                                scope,
                                owner,
                                owner,
                                from,
                                from,
                                until,
                                until);
                var r = p.executeQuery()) {
            while (r.next()) rows.add(read(r));
        }
        return rows;
    }

    // Insert the supplied values using the service's Connection and return the new ID.
    public int insert(Connection c, int owner, LocalDate start, LocalDate end, String notes)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "INSERT INTO cycles(user_id,start_date,period_end_date,notes)"
                                + " VALUES(?,?,?,?)",
                        owner,
                        start,
                        end,
                        notes)) {
            return Jdbc.insert(p);
        }
    }

    // Update the permitted row using bound values; the service controls the transaction.
    public void update(
            Connection c, int id, int scope, LocalDate start, LocalDate end, String notes)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "UPDATE cycles SET start_date=?,period_end_date=?,notes=? WHERE cycle_id=?"
                                + " AND (?=0 OR user_id=?)",
                        start,
                        end,
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
                        "DELETE FROM cycles WHERE cycle_id=? AND (?=0 OR user_id=?)",
                        id,
                        scope,
                        scope)) {
            Jdbc.changed(p);
        }
    }

    // Save the recalculated length; null marks the newest cycle, which has no later start yet.
    public void length(Connection c, int id, int owner, Integer length) throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "UPDATE cycles SET cycle_length=? WHERE cycle_id=? AND user_id=?",
                        length,
                        id,
                        owner)) {
            Jdbc.changed(p);
        }
    }
}
