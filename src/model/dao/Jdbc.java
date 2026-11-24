package model.dao;

import exception.AppException;
import java.sql.*;
import java.time.LocalDate;

final class Jdbc {
    private Jdbc() {}

    static PreparedStatement statement(Connection c, String sql, Object... args)
            throws SQLException {
        PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            // Bind values as parameters rather than concatenating user input into SQL.
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            return p;
        } catch (SQLException e) {
            p.close();
            throw e;
        }
    }

    static int insert(PreparedStatement p) throws SQLException {
        p.executeUpdate();
        try (ResultSet r = p.getGeneratedKeys()) {
            if (!r.next()) throw new SQLException("No generated ID");
            return r.getInt(1);
        }
    }

    static void changed(PreparedStatement p) throws SQLException {
        // A scoped write must affect exactly one accessible record.
        if (p.executeUpdate() != 1)
            throw new AppException("Record not found or access denied. Refresh and retry.");
    }

    static LocalDate date(ResultSet r, String name) throws SQLException {
        Date d = r.getDate(name);
        if (d == null) {
            return null;
        } else {
            return d.toLocalDate();
        }
    }

    static Integer integer(ResultSet r, String name) throws SQLException {
        int v = r.getInt(name);
        // JDBC returns zero for SQL NULL; preserve the distinction in the Java entity.
        if (r.wasNull()) {
            return null;
        } else {
            return v;
        }
    }
}
