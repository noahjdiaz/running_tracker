import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RunRepository {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final String filePath;

    public RunRepository(String filePath) {
        this.filePath = filePath;
    }

    public void save(List<Run> runs) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("# date,distanceMiles,durationSeconds,inputType");
            for (Run r : runs) {
                pw.printf("%s,%.4f,%d,%s%n",
                        r.getDate().format(DATE_FMT),
                        r.getDistanceMiles(),
                        r.getDurationSeconds(),
                        r.getInputType().name()
                );
            }
        } catch (IOException e) {
            System.err.println("Error saving runs: " + e.getMessage());
        }
    }

    public List<Run> load() {
        List<Run> runs = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return runs;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length < 4) {
                    System.err.println("Skipping malformed line " + lineNum);
                    continue;
                }

                LocalDate date = LocalDate.parse(parts[0].trim(), DATE_FMT);
                double distanceMiles = Double.parseDouble(parts[1].trim());
                long duration = Long.parseLong(parts[2].trim());
                Run.InputType type = Run.InputType.valueOf(parts[3].trim());

                runs.add(Run.ofMiles(date, distanceMiles, duration));
            }

        } catch (IOException e) {
            System.err.println("Error loading runs: " + e.getMessage());
        }

        return runs;
    }
}
