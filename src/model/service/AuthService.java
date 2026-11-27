package model.service;

import config.Database;

import exception.AppException;

import model.dao.UserDao;
import model.entity.*;

import utilities.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.Arrays;

/**
 * AuthService.java
 *
 * Handles account registration and login. It validates account details, saves salted password
 * hashes, verifies supplied passwords and creates the signed-in session.
 */
public final class AuthService {
    private final Database db;
    private final UserDao users;
    private final Clock clock;
    // Same PBKDF2 workload for an unknown account. This random dummy is never persisted.
    private final String dummy = Passwords.hash("Unused dummy password".toCharArray());

    public AuthService(Database db, UserDao users, Clock clock) {
        this.db = db;
        this.users = users;
        this.clock = clock;
    }

    // Find the account and verify its hash; a missing account and wrong password use the same
    // message.
    public Session login(String email, char[] password) {
        try {
            String normalized = Validation.email(email);
            return Transactions.run(
                    db,
                    new Transactions.Work<Session>() {
                        @Override
                        public Session run(Connection c) throws SQLException {
                            User u = users.byEmail(c, normalized);
                            // Use a real hash calculation even for unknown emails to avoid a quick
                            // account-existence check.
                            String hash = dummy;
                            if (u != null) {
                                hash = users.password(c, u.id());
                            }
                            boolean valid = Passwords.verify(password, hash);
                            if (!valid || u == null)
                                throw new AppException("Incorrect email or password.");
                            return new Session(u.id(), u.name(), u.role());
                        }
                    });
        } finally {
            // Clear this temporary character array whether authentication succeeds or fails.
            Arrays.fill(password, '\0');
        }
    }

    // Validate both password fields and save a salted hash with the regular user role.
    public int register(String name, String email, char[] password, char[] confirmation) {
        try {
            String n = Validation.text(name, "Name", 100), e = Validation.email(email);
            Validation.require(Arrays.equals(password, confirmation), "Passwords do not match.");
            String hash = Passwords.hash(password);
            return Transactions.run(
                    db,
                    new Transactions.Work<Integer>() {
                        @Override
                        public Integer run(Connection c) throws SQLException {
                            return users.insert(c, n, e, hash, "user", LocalDate.now(clock));
                        }
                    });
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
