package model.service;

import config.Database;

import model.dao.*;
import model.entity.*;

import utilities.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;

/**
 * ReportService.java
 *
 * Validates the report selection, date range and requested owner before running a report DAO
 * query. It supplies the current date for prediction status and chooses summary or detail results.
 */
public final class ReportService {
    private final Database db;
    private final UserDao users;
    private final ReportDao reports;
    private final Session session;
    private final Clock clock;

    public ReportService(
            Database db, UserDao users, ReportDao reports, Session session, Clock clock) {
        this.db = db;
        this.users = users;
        this.reports = reports;
        this.session = session;
        this.clock = clock;
    }

    // Use the injected clock so dates agree with the app's time zone and can be tested.
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // Build a month or year range, check access, then choose the requested report query.
    public ReportData generate(
            String report, Integer owner, Integer year, Integer month, boolean details) {
        Validation.require(year != null && year >= 1 && year <= 9998, "Select a valid year.");
        boolean monthly = report.equals("User Activity") || report.equals("Monthly Symptom");
        LocalDate from;
        LocalDate until;
        // Activity and symptom reports use a month; regularity and accuracy reports use a year.
        if (monthly) {
            from = Filters.month(year, month);
            until = from.plusMonths(1);
        } else {
            from = LocalDate.of(year, 1, 1);
            until = from.plusYears(1);
        }
        return Transactions.run(
                db,
                new Transactions.Work<ReportData>() {
                    @Override
                    public ReportData run(Connection c) throws SQLException {
                        int scope = Access.scope(c, users, session);
                        Filters.owner(owner, scope);
                        if (report.equals("User Activity")) {
                            return reports.activity(c, scope, owner, from, until);
                        } else if (report.equals("Cycle Regularity")) {
                            return reports.regularity(c, scope, owner, from, until);
                        } else if (report.equals("Monthly Symptom")) {
                            return reports.symptoms(c, scope, owner, from, until);
                        } else if (report.equals("Prediction Accuracy")) {
                            if (details) {
                                return reports.accuracyDetails(
                                        c, scope, owner, from, until, today());
                            } else {
                                return reports.accuracy(c, scope, owner, from, until, today());
                            }
                        } else {
                            throw new exception.AppException("Select a report.");
                        }
                    }
                });
    }
}
