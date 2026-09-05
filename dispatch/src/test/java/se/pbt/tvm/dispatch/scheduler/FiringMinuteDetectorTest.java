package se.pbt.tvm.dispatch.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.tvm.core.subscription.SchedulePreset;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FiringMinuteDetector")
class FiringMinuteDetectorTest {

    private static Instant at(int year, int month, int day, int hour, int minute, int second, ZoneId zone) {
        return LocalDateTime.of(year, month, day, hour, minute, second).atZone(zone).toInstant();
    }

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    @DisplayName("Fires exactly at the preset's cron minute, in the preset's own timezone")
    void isFiring_atExactCronMinute_returnsTrue() {
        Instant morning = at(2026, 8, 31, 8, 0, 0, STOCKHOLM);
        assertTrue(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, morning));
    }

    @Test
    @DisplayName("Still fires anywhere within the firing minute (seconds don't matter)")
    void isFiring_laterInSameMinute_returnsTrue() {
        Instant morning = at(2026, 8, 31, 8, 0, 47, STOCKHOLM);
        assertTrue(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, morning));
    }

    @Test
    @DisplayName("Does not fire one minute after the cron minute")
    void isFiring_oneMinuteAfter_returnsFalse() {
        Instant after = at(2026, 8, 31, 8, 1, 0, STOCKHOLM);
        assertFalse(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, after));
    }

    @Test
    @DisplayName("Does not fire one minute before the cron minute")
    void isFiring_oneMinuteBefore_returnsFalse() {
        Instant before = at(2026, 8, 31, 7, 59, 0, STOCKHOLM);
        assertFalse(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, before));
    }

    @Test
    @DisplayName("A preset with multiple daily slots fires at each of them")
    void isFiring_withMultipleDailySlots_firesAtEach() {
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, at(2026, 8, 31, 8, 0, 0, STOCKHOLM)));
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, at(2026, 8, 31, 12, 0, 0, STOCKHOLM)));
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, at(2026, 8, 31, 20, 0, 0, STOCKHOLM)));
        assertFalse(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, at(2026, 8, 31, 14, 0, 0, STOCKHOLM)));
    }

    @Test
    @DisplayName("A US market preset fires at its US local time, independent of the caller's own timezone")
    void isFiring_withUsPreset_firesAtUsLocalTime() {
        // 2026-08-31 is a Monday; 09:30 America/New_York
        Instant usOpen = at(2026, 8, 31, 9, 30, 0, NEW_YORK);
        assertTrue(FiringMinuteDetector.isFiring(SchedulePreset.US_MARKET_OPEN, usOpen));

        // The same instant expressed in Stockholm time would be 15:30 -- confirms the
        // preset's own zone is used, not the caller's.
        assertFalse(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, usOpen));
    }

    @Test
    @DisplayName("Market presets do not fire on weekends")
    void isFiring_withMarketPresetOnWeekend_returnsFalse() {
        // 2026-09-05 is a Saturday
        Instant saturdayOpen = at(2026, 9, 5, 9, 0, 0, STOCKHOLM);
        assertFalse(FiringMinuteDetector.isFiring(SchedulePreset.EUROPE_MARKET_OPEN, saturdayOpen));
    }

    @Test
    @DisplayName("Europe and US market open/close presets fire at their configured local times")
    void isFiring_withMarketPresets_fireAtConfiguredTimes() {
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.EUROPE_MARKET_OPEN, at(2026, 8, 31, 9, 0, 0, STOCKHOLM)));
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.EUROPE_MARKET_CLOSE, at(2026, 8, 31, 17, 30, 0, STOCKHOLM)));
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.US_MARKET_CLOSE, at(2026, 8, 31, 16, 0, 0, NEW_YORK)));
    }
}
