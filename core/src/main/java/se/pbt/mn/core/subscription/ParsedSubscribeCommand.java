package se.pbt.mn.core.subscription;

import java.util.List;

/**
 * The result of parsing a raw "subscribe" request text, before a specific channel (Telegram,
 * email, ...) attaches its own recipient identity to produce a {@link SubscribeCommand}.
 */
public record ParsedSubscribeCommand(
        String language,
        int maxItems,
        List<String> keywords,
        SchedulePreset schedule,
        String email
) {}
