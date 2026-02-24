import java.time.LocalDate;

public class Run {

    public enum InputType {
        MILES, KM, LAPS
    }

    private static final double KM_TO_MILES  = 0.621371;
    private static final double TRACK_STD_FT = 1320.0; // standard 1/4 mile track in feet

    private LocalDate date;
    private double distanceMiles;
    private long durationSeconds;
    private InputType inputType;

    private Run(LocalDate date, double distanceMiles, long durationSeconds, InputType inputType) {
        this.date = date;
        this.distanceMiles = distanceMiles;
        this.durationSeconds = durationSeconds;
        this.inputType = inputType;
    }

    public static Run ofMiles(LocalDate date, double miles, long durationSeconds) {
        return new Run(date, miles, durationSeconds, InputType.MILES);
    }

    public static Run ofKm(LocalDate date, double km, long durationSeconds) {
        return new Run(date, km * KM_TO_MILES, durationSeconds, InputType.KM);
    }

    public static Run ofLaps(LocalDate date, int laps, double trackFeet, long durationSeconds) {
        double miles = (laps * trackFeet) / 5280.0;
        return new Run(date, miles, durationSeconds, InputType.LAPS);
    }

    public LocalDate getDate(){
        return date;
    }
    public double getDistanceMiles() {
        return distanceMiles;
    }
    public double getDistanceKm() {
        return distanceMiles / KM_TO_MILES;
    }
    public long getDurationSeconds() {
        return durationSeconds;
    }
    public InputType getInputType() {
        return inputType;
    }

    public static double standardTrackFeet() {
        return TRACK_STD_FT;
    }

    @Override
    public String toString() {
        return date + " | " + String.format("%.2f", distanceMiles) + " mi"
                + " | " + durationSeconds + " sec"
                + " | entered as: " + inputType;
    }
}
