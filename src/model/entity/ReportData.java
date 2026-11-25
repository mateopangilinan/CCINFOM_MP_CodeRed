package model.entity;

import java.util.List;

/**
 * ReportData.java
 *
 * Carries a report's column headings and data rows from the model to the controller and view. It
 * contains no Swing components or SQL execution logic.
 */
public record ReportData(String[] columns, List<Object[]> rows) {}
