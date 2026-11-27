package model.service;

import config.Database;

import java.sql.*;

/**
 * Transactions.java
 *
 * Runs related DAO operations using one shared database connection. It commits all changes on
 * success and rolls them back if any step fails.
 */
final class Transactions {
    private Transactions() {}

    interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    static <T> T run(Database db, Work<T> work) {
        try (Connection c = db.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            // Every DAO in this operation shares one connection and one commit/rollback boundary.
            c.setAutoCommit(false);
            try {
                T value = work.run(c);
                c.commit();
                return value;
            } catch (SQLException | RuntimeException e) {
                try {
                    // Undo earlier writes too, including calculated values, if any later step
                    // fails.
                    c.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw Database.failure(e);
        }
    }
}
