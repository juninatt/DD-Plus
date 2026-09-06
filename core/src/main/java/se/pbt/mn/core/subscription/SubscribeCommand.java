package se.pbt.mn.core.subscription;

import java.util.List;

/**
 * DTO representing a parsed "subscribe" request, regardless of which channel it arrived
 * through (a Telegram /subscribe command, an inbound subscription email, ...).
 * <p>
 * This is a transport object created by parsing the raw request text, before mapping it
 * into the domain-level Subscription in the subscription module for storage.
 * <p>
 * {@code chatId} is Telegram-shaped and 0 when the request didn't come from Telegram.
 * {@code email} is optional -- when present, the resulting subscription is also delivered
 * via the email channel in addition to Telegram.
 */
public record SubscribeCommand(
        long chatId,
        String language,
        int maxItems,
        List<String> keywords,
        SchedulePreset schedule,
        String email
) {}
