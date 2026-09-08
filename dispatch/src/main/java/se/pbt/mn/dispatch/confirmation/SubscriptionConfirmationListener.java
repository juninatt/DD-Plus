package se.pbt.mn.dispatch.confirmation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.dispatch.grouping.NewsGrouper;
import se.pbt.mn.dispatch.matching.SubscriptionGroupMatcher;
import se.pbt.mn.subscription.event.SubscriptionCreatedEvent;
import se.pbt.mn.subscription.model.Subscription;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sends a one-off confirmation email right after a subscription is created, so an email
 * subscriber gets immediate proof the pipeline works: currently matching news if any exist
 * right now, otherwise a small sample of the most recent news fetched (across every source)
 * so the confirmation always has real content, not just a status line.
 * <p>
 * Email-only -- a Telegram subscriber already gets a synchronous reply from the bot itself,
 * so this only fires when the subscription has an email address configured.
 */
@Component
public class SubscriptionConfirmationListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionConfirmationListener.class);
    private static final String EMAIL_CHANNEL_ID = "email";
    private static final int FALLBACK_SAMPLE_SIZE = 5;
    private static final String FALLBACK_HEADER =
            "Nothing matches your filter yet -- here's a sample of what's available right now:\n\n";
    private static final String NO_NEWS_MESSAGE =
            "Your subscription is active. No news could be fetched right now -- " +
                    "you'll get your first alert during the next scheduled delivery.";

    private final NewsFetcher newsFetcher;
    private final List<NotificationChannel> channels;

    public SubscriptionConfirmationListener(NewsFetcher newsFetcher, List<NotificationChannel> channels) {
        this.newsFetcher = newsFetcher;
        this.channels = channels;
    }

    @EventListener
    public void onSubscriptionCreated(SubscriptionCreatedEvent event) {
        try {
            confirm(event.subscription());
        } catch (Exception e) {
            log.warn("Failed to send subscription confirmation: {}", e.toString());
        }
    }

    private void confirm(Subscription subscription) {
        String email = subscription.getEmail();
        if (email == null || email.isBlank()) {
            return;
        }

        NotificationChannel emailChannel = channels.stream()
                .filter(channel -> EMAIL_CHANNEL_ID.equals(channel.id()))
                .findFirst()
                .orElse(null);
        if (emailChannel == null) {
            log.warn("No email channel registered, cannot send subscription confirmation to {}", email);
            return;
        }

        List<NewsGroup> allGroups = NewsGrouper.group(newsFetcher.fetchAll());
        List<NewsGroup> matched = SubscriptionGroupMatcher.match(allGroups, subscription);

        log.info("Sending subscription confirmation to {} ({})", email,
                matched.isEmpty() ? "no filter matches yet" : matched.size() + " matching item(s)");

        emailChannel.send(email, buildConfirmation(matched, allGroups));
    }

    private Notification buildConfirmation(List<NewsGroup> matched, List<NewsGroup> allGroups) {
        if (!matched.isEmpty()) {
            return notification(describeAll(matched));
        }

        List<NewsGroup> sample = mostRecent(allGroups, FALLBACK_SAMPLE_SIZE);
        if (!sample.isEmpty()) {
            return notification(FALLBACK_HEADER + describeAll(sample));
        }

        return notification(NO_NEWS_MESSAGE);
    }

    private Notification notification(String body) {
        return new Notification("Subscription confirmed", body, null, null, null, null);
    }

    /**
     * The most recently published groups, across every source, newest first. Groups with no
     * publish date sort last.
     */
    private static List<NewsGroup> mostRecent(List<NewsGroup> groups, int limit) {
        return groups.stream()
                .sorted(Comparator.comparing(
                        (NewsGroup group) -> group.primary().publishedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private static String describeAll(List<NewsGroup> groups) {
        return groups.stream()
                .map(SubscriptionConfirmationListener::describe)
                .collect(Collectors.joining("\n\n"));
    }

    private static String describe(NewsGroup group) {
        NewsItem primary = group.primary();
        return primary.url() == null ? primary.title() : primary.title() + "\n" + primary.url();
    }
}
