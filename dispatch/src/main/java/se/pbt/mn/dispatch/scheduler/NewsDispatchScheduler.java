package se.pbt.mn.dispatch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.core.subscription.SchedulePreset;
import se.pbt.mn.dispatch.filter.SubscriptionFilterMatcher;
import se.pbt.mn.dispatch.grouping.NewsGrouper;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Ties subscription schedules to news fetching, filtering, and delivery.
 * <p>
 * Runs a per-minute tick and checks every {@link SchedulePreset} against
 * {@link FiringMinuteDetector} rather than one {@code @Scheduled} method per preset, so
 * adding a new preset later needs no changes here.
 * <p>
 * Note: each {@link SchedulePreset} fires in its own configured timezone (see
 * {@link SchedulePreset#getZone()}); {@link Subscription#getTimezone()} is not applied on
 * top of that yet -- every subscriber on a given preset fires at the same instant
 * regardless of their own timezone setting.
 */
@Component
public class NewsDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsDispatchScheduler.class);

    private final List<NewsSource> sources;
    private final List<NotificationChannel> channels;
    private final SubscriptionService subscriptionService;

    public NewsDispatchScheduler(
            List<NewsSource> sources,
            List<NotificationChannel> channels,
            SubscriptionService subscriptionService
    ) {
        this.sources = sources;
        this.channels = channels;
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedRate = 60_000)
    public void tick() {
        Instant now = Instant.now();
        for (SchedulePreset preset : SchedulePreset.values()) {
            if (FiringMinuteDetector.isFiring(preset, now)) {
                dispatch(preset);
            }
        }
    }

    /**
     * Fetches all sources once, groups items that likely cover the same event, and
     * delivers matching groups to every subscription due for the given preset.
     */
    void dispatch(SchedulePreset preset) {
        List<Subscription> due = subscriptionService.findEnabledBySchedule(preset);
        if (due.isEmpty()) {
            return;
        }

        List<NewsItem> allNews = fetchAll();
        if (allNews.isEmpty()) {
            log.debug("No news fetched for preset={}, skipping {} due subscription(s)", preset, due.size());
            return;
        }

        List<NewsGroup> allGroups = NewsGrouper.group(allNews);

        for (Subscription subscription : due) {
            List<NewsGroup> matched = allGroups.stream()
                    .filter(group -> group.items().stream()
                            .anyMatch(item -> SubscriptionFilterMatcher.matches(item, subscription.getFilter())))
                    .limit(Math.max(subscription.getMaxItems(), 0))
                    .toList();

            for (NewsGroup group : matched) {
                Notification notification = toNotification(group);
                for (NotificationChannel channel : channels) {
                    String recipient = resolveRecipient(channel, subscription);
                    if (recipient != null) {
                        channel.send(recipient, notification);
                    }
                }
            }
        }
    }

    /**
     * Resolves the address a given channel should deliver to for this subscription, or
     * null if the subscriber hasn't configured that channel (e.g. no email set).
     */
    private String resolveRecipient(NotificationChannel channel, Subscription subscription) {
        return switch (channel.id()) {
            case "telegram" -> subscription.getChatId() > 0 ? String.valueOf(subscription.getChatId()) : null;
            case "email" -> subscription.getEmail();
            default -> null;
        };
    }

    /**
     * Fetches every registered source, isolating per-source failures, and dedupes the
     * combined result by {@link NewsItem.ProviderRef}.
     */
    private List<NewsItem> fetchAll() {
        Map<NewsItem.ProviderRef, NewsItem> deduped = new LinkedHashMap<>();
        for (NewsSource source : sources) {
            List<NewsItem> items;
            try {
                items = source.fetchLatest();
            } catch (Exception e) {
                log.warn("News source '{}' failed, skipping: {}", source.id(), e.toString());
                continue;
            }
            for (NewsItem item : items) {
                deduped.putIfAbsent(item.providerRef(), item);
            }
        }
        return List.copyOf(deduped.values());
    }

    /**
     * Builds a Notification from a group's primary (earliest-published) item, listing
     * every distinct source in the group so a subscriber can see a story is corroborated
     * by more than one outlet even though only the primary's link is included.
     */
    private Notification toNotification(NewsGroup group) {
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
