package se.pbt.mn.dispatch.scheduler;

import org.springframework.scheduling.support.CronExpression;
import se.pbt.mn.core.subscription.SchedulePreset;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Determines whether a given instant falls within the firing minute of a
 * {@link SchedulePreset}'s cron expression, evaluated in that preset's own timezone.
 * <p>
 * {@code @Scheduled} annotations can't reference {@code SchedulePreset.MORNING.getCron()}
 * directly (it isn't a compile-time constant), so instead a single per-minute tick checks
 * every preset against this detector. Adding a new preset later needs no scheduler changes.
 * <p>
 * Extracted as a pure function so it can be unit tested against fixed instants instead of
 * waiting on real time.
 */
public final class FiringMinuteDetector {

    private FiringMinuteDetector() {}

    public static boolean isFiring(SchedulePreset preset, Instant now) {
        LocalDateTime nowMinute = now.atZone(preset.getZone()).toLocalDateTime().truncatedTo(ChronoUnit.MINUTES);
        CronExpression cron = CronExpression.parse(preset.getCron());
        LocalDateTime next = cron.next(nowMinute.minusSeconds(1));
        return next != null && !next.isAfter(nowMinute);
    }
}
