package model.service;

import config.Database;

import model.dao.UserDao;
import model.entity.*;

import utilities.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

/**
 * UserService.java
 *
 * Validates and authorizes account listing, creation, editing and deletion. It also controls
 * password changes and protects the final administrator from deletion or demotion.
 */
public final class UserService {
    private final Database db;
    private final UserDao users;
    private final Session session;
    private final Clock clock;

    public UserService(Database db, UserDao users, Session session, Clock clock) {
        this.db = db;
        this.users = users;
        this.session = session;
        this.clock = clock;
    }

    // Validate the filters and current access before asking the DAO for matching records.
    public List<User> list(Integer owner, Integer year, Integer month) {
        LocalDate from = Filters.month(year, month);
        return Transactions.run(
                db,
                new Transactions.Work<List<User>>() {
                    @Override
                    public List<User> run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Filters.owner(owner, scope);
                        LocalDate until = null;
                        if (from != null) {
                            until = from.plusMonths(1);
                        }
                        return users.list(c, scope, owner, from, until);
                    }
                });
    }

    // Check current access and return the requested record, or report it as unavailable.
    public User find(int id) {
        return Transactions.run(
                db,
                new Transactions.Work<User>() {
                    @Override
                    public User run(Connection c) throws SQLException {
                        return Access.found(users.find(c, id, Access.scope(c, users, session)));
                    }
                });
    }

    // Validate input and ownership, then save the new record inside one transaction.
    public int add(String name, String email, char[] password, String role) {
        try {
            String n = Validation.text(name, "Name", 100),
                    e = Validation.email(email),
                    hash = Passwords.hash(password);
            Validation.option(role, new String[] {"user", "admin"});
            return Transactions.run(
                    db,
                    new Transactions.Work<Integer>() {
                        @Override
                        public Integer run(Connection c) throws SQLException {
                            int scope = Access.scope(c, users, session);
                            Validation.require(
                                    scope == 0,
                                    "Only admins may create users here. Use registration to create"
                                            + " your own account.");
                            users.lockAdmins(c);
                            return users.insert(c, n, e, hash, role, LocalDate.now(clock));
                        }
                    });
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    // Check the existing record and its owner before saving the allowed changes.
    public void update(
            int id,
            String name,
            String email,
            String role,
            char[] newPassword,
            char[] currentPassword) {
        try {
            String n = Validation.text(name, "Name", 100), e = Validation.email(email);
            Validation.option(role, new String[] {"user", "admin"});
            String passwordHash;
            // A blank replacement password means keep the existing hash.
            if (newPassword.length == 0) {
                passwordHash = null;
            } else {
                passwordHash = Passwords.hash(newPassword);
            }
            Transactions.run(
                    db,
                    new Transactions.Work<Object>() {
                        @Override
                        public Object run(Connection c) throws SQLException {
                            int scope = Access.scope(c, users, session);
                            // Lock the admin accounts before checking their count so two admins
                            // cannot remove each other at once.
                            int admins = users.lockAdmins(c);
                            scope = Access.scope(c, users, session);
                            Access.lockOwner(c, users, id, scope);
                            User old = Access.found(users.find(c, id, scope));
                            Validation.require(
                                    scope == 0 || old.role().equals(role),
                                    "Only admins may change roles.");
                            Validation.require(
                                    !old.role().equals("admin")
                                            || role.equals("admin")
                                            || admins > 1,
                                    "The final admin cannot be demoted.");
                            if (passwordHash != null && id == session.userId())
                                Validation.require(
                                        Passwords.verify(currentPassword, users.password(c, id)),
                                        "Current password is incorrect.");
                            users.update(c, id, scope, n, e, role, passwordHash);
                            return null;
                        }
                    });
        } finally {
            Arrays.fill(newPassword, '\0');
            Arrays.fill(currentPassword, '\0');
        }
    }

    // Check access before counting the records that an account deletion would remove.
    public int[] affected(int id) {
        return Transactions.run(
                db,
                new Transactions.Work<int[]>() {
                    @Override
                    public int[] run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Access.found(users.find(c, id, scope));
                        return users.affected(c, id, scope);
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
                        Access.scope(c, users, session);
                        int admins = users.lockAdmins(c);
                        int scope = Access.scope(c, users, session);
                        Access.lockOwner(c, users, id, scope);
                        User u = Access.found(users.find(c, id, scope));
                        Validation.require(
                                !u.role().equals("admin") || admins > 1,
                                "The final admin cannot be deleted.");
                        users.delete(c, id, scope);
                        return null;
                    }
                });
    }
}
