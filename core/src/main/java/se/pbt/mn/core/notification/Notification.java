package se.pbt.mn.core.notification;

import java.time.Instant;
import java.util.List;

/**
 * Represents a notification message used as the input model
 * for sending notifications through different channels.
 * <p>
 * {@code source}, {@code publishedAt}, and {@code tickers} are optional context a channel
 * may use to render a richer presentation; a channel is free to ignore any of them.
 */
public record Notification(
        String title,
        String body,
        String url,
        String source,
        Instant publishedAt,
        List<String> tickers
) {}
