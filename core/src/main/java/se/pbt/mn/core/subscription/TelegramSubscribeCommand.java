package se.pbt.mn.core.subscription;

import java.util.List;

/**
 * DTO representing a Telegram /subscribe command.
 * <p>
 * This is a transport object created by parsing the raw Telegram message,
 * before mapping it into the domain-level Subscription in the subscription module for storage.
 * <p>
 * {@code email} is optional -- when present, the resulting subscription is also delivered
 * via the email channel in addition to Telegram.
 */
public record TelegramSubscribeCommand(
        long chatId,
        String language,
        int maxItems,
        List<String> keywords,
        SchedulePreset schedule,
        String email
) {}
