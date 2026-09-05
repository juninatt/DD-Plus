package se.pbt.tvm.dispatch.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.tvm.core.subscription.SchedulePreset;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FiringMinuteDetector")
class FiringMinuteDetectorTest {

    @Test
    @DisplayName("Fires exactly at the preset's cron minute")
    void isFiring_atExactCronMinute_returnsTrue() {
        LocalDateTime morning = LocalDateTime.of(2026, 8, 31, 8, 0, 0);
        assertTrue(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, morning));
    }

    @Test
    @DisplayName("Still fires anywhere within the firing minute (seconds don't matter)")
    void isFiring_laterInSameMinute_returnsTrue() {
        LocalDateTime morning = LocalDateTime.of(2026, 8, 31, 8, 0, 47);
        assertTrue(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, morning));
    }

    @Test
    @DisplayName("Does not fire one minute after the cron minute")
    void isFiring_oneMinuteAfter_returnsFalse() {
        LocalDateTime after = LocalDateTime.of(2026, 8, 31, 8, 1, 0);
        assertFalse(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, after));
    }

    @Test
    @DisplayName("Does not fire one minute before the cron minute")
    void isFiring_oneMinuteBefore_returnsFalse() {
        LocalDateTime before = LocalDateTime.of(2026, 8, 31, 7, 59, 0);
        assertFalse(FiringMinuteDetector.isFiring(SchedulePreset.MORNING, before));
    }

    @Test
    @DisplayName("A preset with multiple daily slots fires at each of them")
    void isFiring_withMultipleDailySlots_firesAtEach() {
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, LocalDateTime.of(2026, 8, 31, 8, 0, 0)));
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, LocalDateTime.of(2026, 8, 31, 12, 0, 0)));
        assertTrue(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, LocalDateTime.of(2026, 8, 31, 20, 0, 0)));
        assertFalse(FiringMinuteDetector.isFiring(
                SchedulePreset.MORNING_LUNCH_EVENING, LocalDateTime.of(2026, 8, 31, 14, 0, 0)));
    }
}
