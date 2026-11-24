package config;

import exception.AppException;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

public final class Database {
    private final String url, user, password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database load() {
        Properties p = new Properties();
        Path file = Path.of("config/application.properties");
        try {
            if (Files.exists(file))
                try (InputStream in = Files.newInputStream(file)) {
                    p.load(in);
                }
        } catch (IOException | IllegalArgumentException e) {
            throw new AppException("Cannot read config/application.properties.");
        }
        return new Database(
                value(p, "CODE_RED_DB_URL", "db.url"),
                value(p, "CODE_RED_DB_USER", "db.user"),
                value(p, "CODE_RED_DB_PASSWORD", "db.password"));
    }

    private static String value(Properties p, String env, String key) {
        String v = System.getenv(env);
        if (v == null) {
            return p.getProperty(key, "");
        } else {
            return v;
        }
    }

    public Connection open() throws SQLException {
        if (url.isBlank() || user.isBlank())
            throw new AppException(
                    "Configure CODE_RED_DB_URL, CODE_RED_DB_USER and CODE_RED_DB_PASSWORD or"
                            + " config/application.properties, then restart CODE RED.");
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(url, user, password);
    }

    public static AppException failure(SQLException e) {
        String state = e.getSQLState();
        if (state == null) {
            state = "";
        }
        if (e.getErrorCode() == 1062)
            return new AppException(
                    "A record with that email, date, or symptom already exists.", e);
        if (state.startsWith("23") || e.getErrorCode() == 3819)
            return new AppException(
                    "This change violates a record constraint. Check dates, values, and related"
                            + " records.",
                    e);
        if (e.getErrorCode() == 1213 || e.getErrorCode() == 1205)
            return new AppException("Another change is in progress. Please retry.", e);
        if (state.startsWith("08") || state.equals("28000"))
            return new AppException(
                    "Cannot connect to MySQL. Check the server, database settings, and account"
                            + " permissions, then retry.",
                    e);
        return new AppException(
                "Database operation failed. Check that the CODE RED schema is installed and the"
                        + " database account has permission.",
                e);
    }
}
