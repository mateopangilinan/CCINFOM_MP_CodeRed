package model.entity;

import java.util.List;

public record ReportData(String[] columns, List<Object[]> rows) {}
