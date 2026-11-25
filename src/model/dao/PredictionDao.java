package model.dao;

import model.entity.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * PredictionDao.java
 *
 * Contains SQL for saved estimates, filtered lists and checks for identical predictions. It
 * reads or changes only records permitted by the scope supplied by the service.
 */
public final class PredictionDao {
    // Convert one database row into an entity, preserving optional values as null.
    private Prediction read(ResultSet r) throws SQLException {
        return new Prediction(
                r.getInt("prediction_id"),
                r.getInt("user_id"),
                Jdbc.date(r, "prediction_date"),
                Jdbc.date(r, "predicted_period_start"),
                Jdbc.date(r, "predicted_period_end"),
                r.getString("prediction_method"),
                r.getInt("confidence_range"));
    }

    // Look up one ID and apply the ownership condition in the same SQL query.
    public Prediction find(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT * FROM cycle_predictions WHERE prediction_id=? AND (?=0 OR"
                                        + " user_id=?)",
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
    public List<Prediction> list(
            Connection c, int scope, Integer owner, LocalDate from, LocalDate until)
            throws SQLException {
        List<Prediction> rows = new ArrayList<>();
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT * FROM cycle_predictions WHERE (?=0 OR user_id=?) AND (? IS"
                                    + " NULL OR user_id=?) AND (? IS NULL OR prediction_date>=?)"
                                    + " AND (? IS NULL OR prediction_date<?) ORDER BY"
                                    + " prediction_date,prediction_id",
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
    public int insert(Connection c, Prediction v) throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "INSERT INTO"
                            + " cycle_predictions(user_id,prediction_date,predicted_period_start,predicted_period_end,prediction_method,confidence_range)"
                            + " VALUES(?,?,?,?,?,?)",
                        v.userId(),
                        v.date(),
                        v.start(),
                        v.end(),
                        v.method(),
                        v.range())) {
            return Jdbc.insert(p);
        }
    }

    // Update the permitted row using bound values; the service controls the transaction.
    public void update(Connection c, int id, int scope, LocalDate start, LocalDate end, int range)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "UPDATE cycle_predictions SET"
                            + " predicted_period_start=?,predicted_period_end=?,confidence_range=?,prediction_method='Manual"
                            + " estimate' WHERE prediction_id=? AND (?=0 OR user_id=?)",
                        start,
                        end,
                        range,
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
                        "DELETE FROM cycle_predictions WHERE prediction_id=? AND (?=0 OR"
                                + " user_id=?)",
                        id,
                        scope,
                        scope)) {
            Jdbc.changed(p);
        }
    }

    // Look for an already saved estimate with the same values before creating a duplicate.
    public Prediction identical(Connection c, Prediction v) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT * FROM cycle_predictions WHERE user_id=? AND"
                                        + " prediction_date=? AND predicted_period_start=? AND"
                                        + " predicted_period_end=? AND prediction_method=? AND"
                                        + " confidence_range=? ORDER BY prediction_id LIMIT 1",
                                v.userId(),
                                v.date(),
                                v.start(),
                                v.end(),
                                v.method(),
                                v.range());
                var r = p.executeQuery()) {
            if (r.next()) {
                return read(r);
            } else {
                return null;
            }
        }
    }
}
