package ch.bfh.ti.hirtp1;


import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Optional;

public class Main {

    private OperatingSystemMXBean operatingSystemMXBean;
    private final List<Battery> batteries;
    private DateTimeFormatter dateTimeFormatter;
    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 1000 * 60;

    public static void main(String[] args) {
        final Main me = new Main();

        try {
            //noinspection InfiniteLoopStatement
            while(true)
            {
                System.out.println(me.getStatusLine());
                Thread.sleep(Main.UPDATE_INTERVAL_IN_MILLISECONDS);
            }
        } catch (InterruptedException ignored) {}
    }

    public Main() {
        this.batteries = Battery.listBatteries();
    }

    public String getStatusLine() {
        return String.format(
                "T:%d°C L:%.2f B:%d%%%c %s|%s",
                this.getTemperatureInCelsius(Paths.get("/sys/devices/virtual/thermal/thermal_zone5/temp")),
                this.getLoadAvg(),
                (this.batteries.get(0).getChargePercentage().orElse(0) + this.batteries.get(1).getChargePercentage().orElse(0)),
                Battery.getChargingDischargingSymbol(this.batteries.get(0).isCharging() || this.batteries.get(1).isCharging()),
                Battery.hoursAndMinutesUntilEmptyOrFull(
                        this.batteries.get(0).minutesUntilEmptyOrFull(Main.UPDATE_INTERVAL_IN_MILLISECONDS) +
                                this.batteries.get(1).minutesUntilEmptyOrFull(Main.UPDATE_INTERVAL_IN_MILLISECONDS)
                ),
                this.getTimeAndDate()
        );
    }

    private int getTemperatureInCelsius(final Path path) {
        return getIntInFile(path).orElse(1000)/1000;
    }

    private double getLoadAvg() {
        if(this.operatingSystemMXBean == null) {
            this.operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        }
        return this.operatingSystemMXBean.getSystemLoadAverage();
    }

    private String getTimeAndDate() {
        if(this.dateTimeFormatter == null) {
            this.dateTimeFormatter = new DateTimeFormatterBuilder()
                    .appendPattern("HH")
                    .appendLiteral(":")
                    .appendPattern("mm")
                    .appendLiteral(" ")
                    .appendLiteral("KW")
                    .appendPattern("w")
                    .appendLiteral(" ")
                    .appendPattern("dd")
                    .appendLiteral(".")
                    .appendPattern("MM")
                    .appendLiteral(".")
                    .appendPattern("YYYY")
                    .toFormatter();
        }
        return ZonedDateTime.now().format(this.dateTimeFormatter);
    }

    public static Optional<Integer> getIntInFile(final Path path) {
        try (final BufferedReader reader = Files.newBufferedReader(path)) {
            return Optional.of(Integer.parseInt(reader.readLine()));
        } catch (IOException e) {
            System.err.println(String.format("Unable to read file %s: %s", path.toString(), e.getMessage()));
        }

        return Optional.empty();

    }

}
