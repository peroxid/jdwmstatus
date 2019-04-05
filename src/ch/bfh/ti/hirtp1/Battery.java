package ch.bfh.ti.hirtp1;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class Battery {
    private final Logger LOG = Logger.getLogger(Battery.class.getCanonicalName());
    private final static String BATTERY_BASE_PATH = "/sys/class/power_supply";
    private final Path myPath;
    private int energyNowPrevious;
    private final int energyFull;



    public Battery(final Path path) {
        this.myPath = Paths.get(Battery.BATTERY_BASE_PATH, path.toString());
        this.energyNowPrevious = this.getEnergyNow().orElse(0);
        this.energyFull = this.getEnergyFull().orElse(0);
    }

    public static List<Battery> listBatteries() {
        return  Arrays.stream(
                Objects.requireNonNull(new File(Battery.BATTERY_BASE_PATH).list(
                        (file, s) -> s.length() == 4 && s.startsWith("BAT")
                ))
        ).map(i -> new Battery(Paths.get(i))).collect(Collectors.toList());
    }

    public static char getChargingDischargingSymbol(final boolean isCharging) {
        return isCharging ? '+' : '-';
    }

    public int minutesUntilEmptyOrFull(final int intervalInMilliseconds) {
        // todo port to civilised language from from https://sourceforge.net/p/acpiclient/code/ci/master/tree/acpi.c#l322
        // the following is wrong
        final Optional<Integer> energyNow = this.getEnergyNow();

        if(energyNow.isPresent()) {
            final int remainingEnergy = this.energyFull - energyNow.get();
            final int difference = energyNow.get() - this.energyNowPrevious;
            this.energyNowPrevious = energyNow.get();
            if(difference == 0) {
                return 0;
            }

            return (int) (Math.abs(((double) remainingEnergy / difference)) / (intervalInMilliseconds * 1000 * 60));
        }

        return 0;
    }


    public static String hoursAndMinutesUntilEmptyOrFull(final int minutesUntilEmptyOrFull) {
        return String.format("%02d:%02d", (minutesUntilEmptyOrFull / 60), (minutesUntilEmptyOrFull % 60));
    }

    public boolean isCharging() {
        try {
            final String contents = Files.readAllLines(Paths.get(this.myPath.toString(), "status")).get(0);
            if(contents.startsWith("Charging")) {
                return true;
            }
            else if(contents.startsWith("Discharging")) {
                return false;
            }
        } catch (IOException e) {
            this.LOG.log(Level.SEVERE, String.format("Unable to read file %s: %s", this.myPath.toString(), e.getMessage()), e);
        }
        return false;
    }

    public Optional<Integer> getChargePercentage() {
        final Optional<Integer> energyFull = this.getEnergyFull();
        final Optional<Integer> energyNow = this.getEnergyNow();
        if(energyFull.isPresent() && energyNow.isPresent()) {
            return Optional.of(new Double(((double) energyNow.get() / energyFull.get()) * 100).intValue());
        }
        this.LOG.log(Level.WARNING, "Unable to detect charge percentage");
        return Optional.empty();
    }


    private Optional<Integer> getIntInFileByKey(final Path path, final String key) {
        try (final BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while((line = reader.readLine()) != null) {
                if(line.startsWith(key)) {
                    return Optional.of(Integer.parseInt(line.substring(line.indexOf("=") + 1)));
                }
            }
        } catch (IOException e) {
            this.LOG.log(Level.SEVERE, String.format("Unable to read file %s: %s", path.toString(), e.getMessage()), e);
            return Optional.empty();
        }

        this.LOG.log(Level.SEVERE, String.format("Found no line starting with %s in file %s", key, path.toString()));
        return Optional.empty();
    }

    private Optional<Integer> getEnergyFull() {
        return this.getIntInFileByKey(Paths.get(this.myPath.toString(), "uevent"), "POWER_SUPPLY_ENERGY_FULL");
    }

    private Optional<Integer> getEnergyNow() {
        return this.getIntInFileByKey(Paths.get(this.myPath.toString(), "uevent"), "POWER_SUPPLY_ENERGY_NOW");
    }

}
