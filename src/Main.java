import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static Scanner sc = new Scanner(System.in);
    static RunService service = new RunService(new RunRepository("runs.csv"));

    public static void main(String[] args) {
        System.out.println("Welcome to RunTracker!");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> logRun();
                case "2" -> showStats();
                case "3" -> showHistory();
                case "4" -> {
                    System.out.println("Stay consistent. See you next run! ");
                    running = false;
                }
                default -> System.out.println("Invalid option, try again.");
            }
        }
        sc.close();
    }

//menu
    static void printMenu() {
        System.out.println("""

            ==============================
                   ** RUN TRACKER **
            ==============================
              1. Log a run
              2. Stats & goals
              3. Run history
              4. Exit
            ==============================""");
        System.out.print("  Choice: ");
    }

//log
    static void logRun() {
        System.out.println(" Log a Run");
        LocalDate date = promptDate();

        System.out.print("Duration [hh:mm:ss or mm:ss, or Enter to skip]: ");
        long duration = parseDuration(sc.nextLine().trim());

        Run run = promptDistance(duration);
        Run dated = Run.ofMiles(date, run.getDistanceMiles(), duration);
        service.addRun(dated);

        System.out.println("Run saved!");
        printRunSummary(dated);
        showGoals();
    }


    static Run promptDistance(long durationSeconds) {
        while (true) {
            System.out.println("How did you measure your run?");
            System.out.println("  1. Miles");
            System.out.println("  2. Kilometers");
            System.out.println("  3. Laps");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    double miles = promptPositiveDouble("Distance (miles): ");
                    return Run.ofMiles(LocalDate.now(), miles, durationSeconds);
                }
                case "2" -> {
                    double km = promptPositiveDouble("Distance (km): ");
                    return Run.ofKm(LocalDate.now(), km, durationSeconds);
                }
                case "3" -> {
                    int laps = promptPositiveInt("Number of laps: ");

                    System.out.println("Track size:");
                    System.out.println("  1. Standard outdoor track (1320 ft / 0.25 mi)");
                    System.out.println("  2. Standard indoor track  (200 m / ~656 ft)");
                    System.out.println("  3. Enter custom track size in feet");
                    System.out.println("  4. Enter custom track size in meters");
                    System.out.print("  Choice: ");
                    String trackChoice = sc.nextLine().trim();

                    double trackFeet = switch (trackChoice) {
                        case "1" -> Run.standardTrackFeet();
                        case "2" -> 200 * 3.28084;
                        case "3" -> promptPositiveDouble("Track length (feet): ");
                        case "4" -> promptPositiveDouble("Track length (meters): ") * 3.28084;
                        default  -> {
                            System.out.println("⚠  Invalid, using standard outdoor track.");
                            yield Run.standardTrackFeet();
                        }
                    };

                    double lapsPerMile = 5280.0 / trackFeet;
                    System.out.printf("  → %.1f laps = 1 mile on this track%n", lapsPerMile);
                    System.out.printf("  → Your %d laps = %.2f miles%n",
                            laps, (laps * trackFeet) / 5280.0);

                    return Run.ofLaps(LocalDate.now(), laps, trackFeet, durationSeconds);
                }
                default -> System.out.println("⚠  Please enter 1, 2, or 3.");
            }
        }
    }


    static void showStats() {
        String div = "=".repeat(46);
        System.out.println(div);
        System.out.println("               ** YOUR STATS **");
        System.out.println(div);
        printStatBlock("This Week  (Sun-Sat)", service.getThisCalendarWeek());
        printStatBlock("Last 7 Days", service.getLast7Days());
        printStatBlock("Last 30 Days", service.getLast30Days());
        printStatBlock("Last 365 Days", service.getLast365Days());
        printStatBlock("Calendar Year", service.getCalendarYear());
        printStatBlock("All Time", service.getAllTime());
        System.out.println(div);
        showGoals();
    }

    static void printStatBlock(String label, RunService.RunStats s) {
        System.out.println("  [ " + label + " ]");
        System.out.printf("    Runs: %-5d  Total: %-6.2f mi  Avg: %-5.2f mi%n",
                s.totalRuns(), s.totalMiles(), s.avgMiles());
        System.out.printf("    Best single day: %.2f mi%n", s.highestDay());
        System.out.println("-".repeat(46));
    }

    static void showGoals() {
        double nextRun = service.getNextRunGoalMiles();
        System.out.printf("%n Next run goal:  %.2f mi", nextRun);
        if (nextRun == RunService.HALF_MARATHON) System.out.print("   Half Marathon!");
        if (nextRun == RunService.MARATHON)      System.out.print("   Full Marathon!");
        System.out.println();

        System.out.print("Runs left this week? [Enter for default (1 per remaining day)]: ");
        String input = sc.nextLine().trim();
        int runsLeft = 0;
        if (!input.isEmpty()) {
            try { runsLeft = Integer.parseInt(input); }
            catch (NumberFormatException e) { System.out.println("⚠  Invalid, using default."); }
        }

        RunService.WeeklyGoalBreakdown wb = service.getWeeklyGoalBreakdown(runsLeft);
        System.out.println();
        System.out.println(" Weekly Goal Breakdown");
        System.out.println("─".repeat(42));
        System.out.printf("  Week target:      %.2f mi%n",  wb.weeklyGoalMiles());
        System.out.printf("  Already run:      %.2f mi%n",  wb.milesCompletedThisWeek());
        System.out.printf("  Miles remaining:  %.2f mi%n",  wb.milesRemaining());
        System.out.printf("  Runs planned:     %d%n",       wb.runsRemaining());
        System.out.printf("  Miles per run:    %.2f mi%n",  wb.milesPerRun());
        System.out.println("─".repeat(42));
    }

    static void showHistory() {
        var runs = service.getAllRuns();
        if (runs.isEmpty()) { System.out.println("No runs logged yet."); return; }

        System.out.println("\n Run History (most recent first)");
        System.out.println("─".repeat(58));
        System.out.printf("%-12s  %-10s  %-10s  %-10s  %-6s%n",
                "Date", "Miles", "Duration", "Pace/mi", "Type");
        System.out.println("─".repeat(58));

        runs.stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(25)
                .forEach(r -> {
                    String dur  = r.getDurationSeconds() > 0 ? formatDuration(r.getDurationSeconds()) : "─";
                    String pace = r.getDurationSeconds() > 0
                            ? String.format("%.1f min", RunService.paceMinPerMile(r)) : "─";
                    System.out.printf("%-12s  %-10.2f  %-10s  %-10s  %-6s%n",
                            r.getDate().format(DATE_FMT), r.getDistanceMiles(),
                            dur, pace, r.getInputType());
                });

        if (runs.size() > 25)
            System.out.printf("  ... and %d more in file.%n", runs.size() - 25);
        System.out.println("─".repeat(58));
    }

    static void printRunSummary(Run r) {
        System.out.println("\n--- Run Summary -------------------");
        System.out.printf("  Date:       %s%n",       r.getDate().format(DATE_FMT));
        System.out.printf("  Distance:   %.2f mi%n",  r.getDistanceMiles());
        if (r.getDurationSeconds() > 0) {
            System.out.printf("  Duration:   %s%n",   formatDuration(r.getDurationSeconds()));
            System.out.printf("  Pace:       %.2f min/mi%n", RunService.paceMinPerMile(r));
        }
        System.out.printf("  Entered as: %s%n",       r.getInputType());
        System.out.println("-----------------------------------");
    }

    static LocalDate promptDate() {
        while (true) {
            System.out.print("Date [today / yyyy-MM-dd]: ");
            String input = sc.nextLine().trim();
            if (input.equalsIgnoreCase("today") || input.isEmpty()) {
                LocalDate today = LocalDate.now();
                System.out.println("  → " + today.format(DATE_FMT));
                return today;
            }
            try { return LocalDate.parse(input, DATE_FMT); }
            catch (DateTimeParseException e) {
                System.out.println("Use 'today' or yyyy-MM-dd (e.g. 2025-04-20).");
            }
        }
    }


    static double promptPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(sc.nextLine().trim());
                if (v > 0) return v;
                System.out.println("Must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Enter a number (e.g. 3.1).");
            }
        }
    }

    static int promptPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (v > 0) return v;
                System.out.println("Must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    static String formatDuration(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    static long parseDuration(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            String[] parts = s.split(":");
            if (parts.length == 2) return Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1]);
            if (parts.length == 3) return Long.parseLong(parts[0]) * 3600
                    + Long.parseLong(parts[1]) * 60
                    + Long.parseLong(parts[2]);
        } catch (NumberFormatException ignored) {}
        System.out.println(" Could not parse duration, skipping.");
        return 0;
    }
}
