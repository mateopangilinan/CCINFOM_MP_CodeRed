package model.service;

import model.entity.Cycle;

import utilities.Validation;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * CycleRules.java
 *
 * Calculates cycle lengths and both user averages from an ordered history of cycle starts and
 * known period ends. It performs calculations only, leaving database reads and writes to the
 * service and DAOs.
 */
public final class CycleRules {
    private CycleRules() {}

    public record History(List<Cycle> cycles, BigDecimal cycleAverage, BigDecimal periodAverage) {}

    // Sort all starts first so adding or editing an older cycle also corrects later lengths.
    public static History calculate(List<Cycle> input, Clock clock) {
        List<Cycle> sorted = new ArrayList<>(input);
        sorted.sort(
                new Comparator<Cycle>() {
                    public int compare(Cycle first, Cycle second) {
                        int dateOrder = first.start().compareTo(second.start());
                        if (dateOrder != 0) {
                            return dateOrder;
                        }
                        return Integer.compare(first.id(), second.id());
                    }
                });
        List<Cycle> result = new ArrayList<>();
        // Keep completed cycle gaps and known bleeding durations in separate lists for their
        // averages.
        List<Integer> lengths = new ArrayList<>(), periods = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Cycle v = sorted.get(i);
            Validation.actual(v.start(), clock);
            if (v.end() != null) {
                Validation.actual(v.end(), clock);
                Validation.require(
                        !v.end().isBefore(v.start()), "Period end cannot precede its start.");
                // Include both the start and end day when counting the number of bleeding days.
                periods.add(Math.toIntExact(ChronoUnit.DAYS.between(v.start(), v.end()) + 1));
            }
            // Leave the latest length null because there is no later start from which to measure a
            // gap.
            Integer length = null;
            if (i + 1 < sorted.size()) {
                // A cycle length is the number of days from this start to the following start.
                LocalDate next = sorted.get(i + 1).start();
                length = Math.toIntExact(ChronoUnit.DAYS.between(v.start(), next));
                Validation.require(length > 0, "A cycle already starts on that date.");
                Validation.require(
                        v.end() == null || v.end().isBefore(next),
                        "A completed period cannot overlap the next cycle start.");
                lengths.add(length);
            }
            result.add(new Cycle(v.id(), v.userId(), v.start(), v.end(), length, v.notes()));
        }
        return new History(List.copyOf(result), average(lengths), average(periods));
    }

    // Calculate a mean rounded to one decimal place; no values means no average.
    public static BigDecimal average(List<Integer> values) {
        if (values.isEmpty()) return null;
        BigDecimal total = BigDecimal.ZERO;
        for (Integer value : values) {
            total = total.add(BigDecimal.valueOf(value));
        }
        BigDecimal result =
                total.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
        Validation.require(
                result.signum() > 0 && result.compareTo(new BigDecimal("999.9")) <= 0,
                "Calculated average cannot fit DECIMAL(4,1); review the cycle history.");
        return result;
    }
}
