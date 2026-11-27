package model.service;

import model.entity.*;

import utilities.Validation;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * PredictionRules.java
 *
 * Calculates period estimates from recorded history and matches saved estimates to eligible
 * actual cycles. It contains the rounding, matching-window and tie-breaking rules without accessing
 * the database.
 */
public final class PredictionRules {
    private PredictionRules() {}

    // Use this owner's saved history to create an estimate; reject history that cannot support one.
    public static Prediction estimate(int owner, List<Cycle> history, LocalDate today) {
        // First keep only the requested owner's history, then put the newest starts first.
        List<Cycle> sorted = new ArrayList<>();
        for (Cycle cycle : history) {
            if (cycle.userId() == owner) {
                sorted.add(cycle);
            }
        }
        sorted.sort(
                new Comparator<Cycle>() {
                    public int compare(Cycle first, Cycle second) {
                        return second.start().compareTo(first.start());
                    }
                });
        // Use at most three completed cycle gaps; a missing length is not treated as zero.
        List<Integer> lengths = new ArrayList<>();
        for (Cycle cycle : sorted) {
            if (cycle.length() != null && lengths.size() < 3) {
                lengths.add(cycle.length());
            }
        }
        Validation.require(
                !lengths.isEmpty(),
                "At least one completed cycle length is required (record two cycle starts).");
        // Use all known period durations in this history, counting the start and end days.
        List<Long> periods = new ArrayList<>();
        for (Cycle cycle : sorted) {
            if (cycle.end() != null) {
                periods.add(ChronoUnit.DAYS.between(cycle.start(), cycle.end()) + 1);
            }
        }
        Validation.require(
                !periods.isEmpty(),
                "At least one known period duration is required (record a period end date).");
        long lengthTotal = 0;
        for (Integer length : lengths) {
            lengthTotal += length;
        }
        long periodTotal = 0;
        for (Long period : periods) {
            periodTotal += period;
        }
        long length = rounded(lengthTotal, lengths.size());
        long duration = rounded(periodTotal, periods.size());
        // Add the rounded cycle average to the newest recorded start to estimate the next start.
        LocalDate start = sorted.getFirst().start().plusDays(length);
        Validation.require(
                start.isAfter(today),
                "The estimated start has passed or is today. Review your cycle history; no"
                        + " prediction was saved.");
        // Round half the spread between the longest and shortest sampled cycle gaps upward.
        int range =
                (int) Math.ceil((Collections.max(lengths) - (double) Collections.min(lengths)) / 2);
        String cycleWord = "cycles";
        if (lengths.size() == 1) {
            cycleWord = "cycle";
        }
        // Save these calculated values as a snapshot; later history changes do not rewrite this
        // estimate.
        return new Prediction(
                0,
                owner,
                today,
                start,
                start.plusDays(duration - 1),
                "Average of last " + lengths.size() + " " + cycleWord,
                range);
    }

    // Round a mean to a whole number of days, with .5 rounded upward.
    private static long rounded(long sum, int count) {
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public record Match(String status, Cycle cycle, Integer error) {}

    // Choose the closest eligible actual cycle; no eligible cycle leaves the estimate pending or
    // unmatched.
    public static Match match(Prediction prediction, List<Cycle> cycles, LocalDate today) {
        Cycle best = null;
        long bestError = Long.MAX_VALUE;
        for (Cycle cycle : cycles) {
            // Ignore other owners and any cycle that did not start strictly after the prediction
            // was made.
            if (cycle.userId() != prediction.userId()
                    || !cycle.start().isAfter(prediction.date())) {
                continue;
            }
            long error = Math.abs(ChronoUnit.DAYS.between(prediction.start(), cycle.start()));
            // A difference of exactly 14 days is eligible; a larger difference is outside the
            // matching window.
            if (error > 14) {
                continue;
            }
            if (best == null || error < bestError) {
                best = cycle;
                bestError = error;
            } else if (error == bestError) {
                // Equal errors choose the earlier actual start, then the lower cycle ID.
                if (cycle.start().isBefore(best.start())
                        || (cycle.start().equals(best.start()) && cycle.id() < best.id())) {
                    best = cycle;
                }
            }
        }
        if (best != null) {
            return new Match("Matched", best, (int) bestError);
        }
        // An unmatched estimate stays pending through day +14; only the following day is it Not
        // matched.
        if (today.isAfter(prediction.start().plusDays(14))) {
            return new Match("Not matched", null, null);
        } else {
            return new Match("Pending", null, null);
        }
    }
}
