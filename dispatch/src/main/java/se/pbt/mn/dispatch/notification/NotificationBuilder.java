package se.pbt.mn.dispatch.notification;

import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds a {@link Notification} from a {@link NewsGroup}.
 */
public final class NotificationBuilder {

    private NotificationBuilder() {}

    /**
     * Builds a Notification from a group's primary (earliest-published) item, listing
     * every distinct source in the group so a subscriber can see a story is corroborated
     * by more than one outlet even though only the primary's link is included.
     */
    public static Notification fromGroup(NewsGroup group) {
        NewsItem primary = group.primary();

        String sources = group.items().stream()
                .map(NewsItem::source)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));

        return new Notification(
                primary.title(),
                primary.description(),
                primary.url() == null ? null : primary.url().toString(),
                sources.isBlank() ? null : sources,
                primary.publishedAt(),
                primary.tickers()
        );
    }
}
