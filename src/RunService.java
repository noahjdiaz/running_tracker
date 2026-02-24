import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RunService {

    public static final double HALF_MARATHON = 13.1;
    public static final double MARATHON      = 26.2;

    private List<Run> runs;
    private RunRepository repository;

    public RunService(RunRepository repository) {
        this.repository = repository;
        this.runs       = repository.load();
    }

    public void addRun(Run run) {
        runs.add(run);
        repository.save(runs);
    }

    public List<Run> getAllRuns() {
        return runs;
    }

    public RunStats getStats(LocalDate from, LocalDate to) {
        List<Run> filtered = runs.stream()
                .filter(r -> !r.getDate().isBefore(from) && !r.getDate().isAfter(to))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) return new RunStats(0, 0.0, 0.0, 0.0);

        int    totalRuns  = filtered.size();
        double totalMiles = filtered.stream().mapToDouble(Run::getDistanceMiles).sum();
        double avgMiles   = totalMiles / totalRuns;
        double highestDay = filtered.stream().mapToDouble(Run::getDistanceMiles).max().orElse(0.0);

        return new RunStats(totalRuns, totalMiles, avgMiles, highestDay);
    }

    public RunStats getLast7Days() {
        LocalDate today = LocalDate.now();
        return getStats(today.minusDays(7), today);
    }

    public RunStats getLast30Days() {
        LocalDate today = LocalDate.now();
        return getStats(today.minusDays(30), today);
    }

    public RunStats getLast365Days() {
        LocalDate today = LocalDate.now();
        return getStats(today.minusDays(365), today);
    }

    public RunStats getCalendarYear() {
        LocalDate today    = LocalDate.now();
        LocalDate janFirst = LocalDate.of(today.getYear(), 1, 1);
        return getStats(janFirst, today);
    }

    public RunStats getAllTime() {
        if (runs.isEmpty()) return new RunStats(0, 0.0, 0.0, 0.0);
        LocalDate earliest = runs.stream().map(Run::getDate).min(Comparator.naturalOrder()).get();
        return getStats(earliest, LocalDate.now());
    }

    public LocalDate getWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    public LocalDate getWeekEnd() {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    }

    public RunStats getThisCalendarWeek() {
        return getStats(getWeekStart(), getWeekEnd());
    }

    public double getNextRunGoalMiles() {
        Optional<Run> lastRun = runs.stream()
                .max(Comparator.comparing(Run::getDate));

        double last = lastRun.map(Run::getDistanceMiles).orElse(0.0);

        if (last >= 23.0) return MARATHON;
        if (last >= 10.0) return HALF_MARATHON;

        double increment;
        if      (last < 1.5)  increment = 0.25;
        else if (last < 3.0)  increment = 0.50;
        else if (last < 10.0) increment = 1.00;
        else                  increment = 2.00;

        double goal = last + increment;

        if (goal >= 10.0 && last < 10.0) return HALF_MARATHON;
        if (goal >= 23.0 && last < 23.0) return MARATHON;

        return Math.round(goal * 100.0) / 100.0;
    }

    public double getNextWeeklyGoalMiles() {
        LocalDate lastWeekStart = getWeekStart().minusWeeks(1);
        LocalDate lastWeekEnd   = getWeekStart().minusDays(1);
        RunStats  lastWeek      = getStats(lastWeekStart, lastWeekEnd);

        double lastTotal = lastWeek.totalMiles();

        double increment = lastTotal < 25.0 ? 3.0 : 5.0;
        return Math.round((lastTotal + increment) * 100.0) / 100.0;
    }

    public int getRemainingDaysInWeek() {
        LocalDate today   = LocalDate.now();
        LocalDate weekEnd = getWeekEnd();
        long days = today.until(weekEnd, java.time.temporal.ChronoUnit.DAYS);
        return (int) Math.max(0, days);
    }

    public WeeklyGoalBreakdown getWeeklyGoalBreakdown(int plannedRunsRemaining) {
        double weeklyGoal     = getNextWeeklyGoalMiles();
        double milesThisWeek  = getThisCalendarWeek().totalMiles();
        double milesRemaining = Math.max(0, weeklyGoal - milesThisWeek);

        int runsToUse = plannedRunsRemaining > 0
                ? plannedRunsRemaining
                : Math.max(1, getRemainingDaysInWeek());

        double milesPerRun = Math.round((milesRemaining / runsToUse) * 100.0) / 100.0;

        return new WeeklyGoalBreakdown(weeklyGoal, milesThisWeek, milesRemaining,
                runsToUse, milesPerRun);
    }


    public static double paceMinPerMile(Run run) {
        if (run.getDurationSeconds() == 0 || run.getDistanceMiles() == 0) return 0;
        return (run.getDurationSeconds() / 60.0) / run.getDistanceMiles();
    }

    public record RunStats(int totalRuns, double totalMiles, double avgMiles, double highestDay) {}

    public record WeeklyGoalBreakdown(
            double weeklyGoalMiles,
            double milesCompletedThisWeek,
            double milesRemaining,
            int    runsRemaining,
            double milesPerRun) {}
}