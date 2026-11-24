package model.dao;

import model.entity.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class UserDao {
    private static final String COLUMNS =
            "user_id,name,email,avg_cycle_length,avg_period_length,created_at,role";

    private User read(ResultSet r) throws SQLException {
        return new User(
                r.getInt("user_id"),
                r.getString("name"),
                r.getString("email"),
                r.getBigDecimal("avg_cycle_length"),
                r.getBigDecimal("avg_period_length"),
                Jdbc.date(r, "created_at"),
                r.getString("role"));
    }

    public User byEmail(Connection c, String email) throws SQLException {
        try (var p = Jdbc.statement(c, "SELECT " + COLUMNS + " FROM users WHERE email=?", email);
                var r = p.executeQuery()) {
            if (r.next()) {
                return read(r);
            } else {
                return null;
            }
        }
    }

    public String password(Connection c, int id) throws SQLException {
        try (var p = Jdbc.statement(c, "SELECT password FROM users WHERE user_id=?", id);
                var r = p.executeQuery()) {
            if (r.next()) {
                return r.getString(1);
            } else {
                return null;
            }
        }
    }

    public User find(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT "
                                        + COLUMNS
                                        + " FROM users WHERE user_id=? AND (?=0 OR user_id=?)",
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

    public List<User> list(Connection c, int scope, Integer owner, LocalDate from, LocalDate until)
            throws SQLException {
        List<User> rows = new ArrayList<>();
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT "
                                        + COLUMNS
                                        + " FROM users WHERE (?=0 OR user_id=?) AND (? IS NULL OR"
                                        + " user_id=?) AND (? IS NULL OR created_at>=?) AND (? IS"
                                        + " NULL OR created_at<?) ORDER BY user_id",
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

    public int insert(
            Connection c, String name, String email, String hash, String role, LocalDate date)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "INSERT INTO users(name,email,password,role,created_at) VALUES(?,?,?,?,?)",
                        name,
                        email,
                        hash,
                        role,
                        date)) {
            return Jdbc.insert(p);
        }
    }

    public void update(
            Connection c, int id, int scope, String name, String email, String role, String hash)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "UPDATE users SET name=?,email=?,role=?,password=COALESCE(?,password) WHERE"
                                + " user_id=? AND (?=0 OR user_id=?)",
                        name,
                        email,
                        role,
                        hash,
                        id,
                        scope,
                        scope)) {
            Jdbc.changed(p);
        }
    }

    public void delete(Connection c, int id, int scope) throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "DELETE FROM users WHERE user_id=? AND (?=0 OR user_id=?)",
                        id,
                        scope,
                        scope)) {
            Jdbc.changed(p);
        }
    }

    public boolean lock(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT user_id FROM users WHERE user_id=? AND (?=0 OR user_id=?)"
                                        + " FOR UPDATE",
                                id,
                                scope,
                                scope);
                var r = p.executeQuery()) {
            return r.next();
        }
    }

    public int lockAdmins(Connection c) throws SQLException {
        int count = 0;
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT user_id FROM users WHERE role='admin' ORDER BY user_id FOR"
                                        + " UPDATE");
                var r = p.executeQuery()) {
            while (r.next()) count++;
        }
        return count;
    }

    public void averages(Connection c, int id, BigDecimal cycle, BigDecimal period)
            throws SQLException {
        try (var p =
                Jdbc.statement(
                        c,
                        "UPDATE users SET avg_cycle_length=?,avg_period_length=? WHERE user_id=?",
                        cycle,
                        period,
                        id)) {
            Jdbc.changed(p);
        }
    }

    public int[] affected(Connection c, int id, int scope) throws SQLException {
        try (var p =
                        Jdbc.statement(
                                c,
                                "SELECT (SELECT COUNT(*) FROM cycles WHERE user_id=u.user_id),"
                                    + " (SELECT COUNT(*) FROM daily_logs WHERE user_id=u.user_id),"
                                    + " (SELECT COUNT(*) FROM symptoms s JOIN daily_logs l ON"
                                    + " s.log_id=l.log_id WHERE l.user_id=u.user_id), (SELECT"
                                    + " COUNT(*) FROM cycle_predictions WHERE user_id=u.user_id)"
                                    + " FROM users u WHERE u.user_id=? AND (?=0 OR u.user_id=?)",
                                id,
                                scope,
                                scope);
                var r = p.executeQuery()) {
            if (!r.next()) return new int[4];
            return new int[] {r.getInt(1), r.getInt(2), r.getInt(3), r.getInt(4)};
        }
    }
}
