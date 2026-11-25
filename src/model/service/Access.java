package model.service;

import model.dao.UserDao;
import model.entity.*;

import utilities.Validation;

import java.sql.*;

/**
 * Access.java
 *
 * Checks whether the signed-in account still exists with the expected role and determines which
 * owner IDs it may access. It also locks owner rows when related changes need to happen safely
 * together.
 */
final class Access {
    private Access() {}

    // Return 0 for an admin, or the signed-in user ID for an owner-only SQL query.
    static int scope(Connection c, UserDao users, Session session) throws SQLException {
        User current = users.find(c, session.userId(), session.userId());
        Validation.require(
                current != null && current.role().equals(session.role()),
                "Your account permissions changed. Please sign out and sign in again.");
        if (session.admin()) {
            return 0;
        } else {
            return session.userId();
        }
    }

    // Check ownership and lock the account row so competing changes for that owner wait.
    static void lockOwner(Connection c, UserDao users, int owner, int scope) throws SQLException {
        Validation.require(
                owner > 0 && (scope == 0 || scope == owner), "Access denied for this user.");
        Validation.require(users.lock(c, owner, scope), "User not found or access denied.");
    }

    static <T> T found(T value) {
        Validation.require(value != null, "Record not found or access denied.");
        return value;
    }
}
