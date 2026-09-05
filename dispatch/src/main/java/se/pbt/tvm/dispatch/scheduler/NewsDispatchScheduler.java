package se.pbt.tvm.dispatch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.pbt.tvm.core.news.NewsItem;
import se.pbt.tvm.core.news.NewsSource;
import se.pbt.tvm.core.notification.Notification;
import se.pbt.tvm.core.notification.NotificationChannel;
import se.pbt.tvm.core.subscription.SchedulePreset;
import se.pbt.tvm.dispatch.filter.SubscriptionFilterMatcher;
import se.pbt.tvm.subscription.model.Subscription;
import se.pbt.tvm.subscription.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ties subscription schedules to news fetching, filtering, and delivery.
 * <p>
 * Runs a per-minute tick and checks every {@link SchedulePreset} against
 * {@link FiringMinuteDetector} rather than one {@code @Scheduled} method per preset, so
 * adding a new preset later needs no changes here.
 * <p>
 * Note: {@link Subscription#getTimezone()} is not applied to firing detection yet — all
 * subscriptions on a given preset fire together in the server's default timezone.
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
        LocalDateTime now = LocalDateTime.now();
        for (SchedulePreset preset : SchedulePreset.values()) {
            if (FiringMinuteDetector.isFiring(preset, now)) {
                dispatch(preset);
            }
        }
    }

    /**
     * Fetches all sources once and delivers matching items to every subscription due for
     * the given preset.
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

        for (Subscription subscription : due) {
            List<NewsItem> matched = allNews.stream()
                    .filter(item -> SubscriptionFilterMatcher.matches(item, subscription.getFilter()))
                    .limit(Math.max(subscription.getMaxItems(), 0))
                    .toList();

            for (NewsItem item : matched) {
                Notification notification = toNotification(item);
                for (NotificationChannel channel : channels) {
                    channel.send(subscription.getChatId(), notification);
                }
            }
        }
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

    private Notification toNotification(NewsItem item) {
        return new Notification(
                item.title(),
                item.description(),
                item.url() == null ? null : item.url().toString()
        );
    }
}
