package se.pbt.mn.core.subscription;

import lombok.Getter;

import java.time.ZoneId;

/**
 * Represents predefined schedule presets that are translated into cron expressions.
 * <p>
 * Each preset carries its own {@link ZoneId} so it fires at the correct local time
 * regardless of the server's own timezone -- a US market preset should fire at 09:30
 * America/New_York, not 09:30 wherever the app happens to be deployed, and the two
 * zones don't even shift for daylight saving on the same calendar days.
 */
@Getter
public enum SchedulePreset {
    MORNING("0 0 8 * * *", ZoneId.of("Europe/Stockholm")),
    EVENING("0 0 20 * * *", ZoneId.of("Europe/Stockholm")),
    MORNING_EVENING("0 0 8,20 * * *", ZoneId.of("Europe/Stockholm")),
    MORNING_LUNCH_EVENING("0 0 8,12,20 * * *", ZoneId.of("Europe/Stockholm")),

    /** Nordic/European market open (weekdays only), e.g. Nasdaq Stockholm at 09:00 CET/CEST. */
    EUROPE_MARKET_OPEN("0 0 9 * * MON-FRI", ZoneId.of("Europe/Stockholm")),
    /** Nordic/European market close (weekdays only), e.g. Nasdaq Stockholm at 17:30 CET/CEST. */
    EUROPE_MARKET_CLOSE("0 30 17 * * MON-FRI", ZoneId.of("Europe/Stockholm")),
    /** US market open (weekdays only), NYSE/NASDAQ at 09:30 America/New_York. */
    US_MARKET_OPEN("0 30 9 * * MON-FRI", ZoneId.of("America/New_York")),
    /** US market close (weekdays only), NYSE/NASDAQ at 16:00 America/New_York. */
    US_MARKET_CLOSE("0 0 16 * * MON-FRI", ZoneId.of("America/New_York"));

    private final String cron;
    private final ZoneId zone;

    SchedulePreset(String cron, ZoneId zone) {
        this.cron = cron;
        this.zone = zone;
    }
}
