package se.pbt.mn.dispatch.confirmation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.core.subscription.SchedulePreset;
import se.pbt.mn.dispatch.fetch.NewsFetcher;
import se.pbt.mn.subscription.event.SubscriptionCreatedEvent;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.model.SubscriptionFilter;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("SubscriptionConfirmationListener")
class SubscriptionConfirmationListenerTest {

    private NewsFetcher newsFetcher;
    private NotificationChannel emailChannel;
    private SubscriptionConfirmationListener listener;

    @BeforeEach
    void setUp() {
        newsFetcher = mock(NewsFetcher.class);
        emailChannel = mock(NotificationChannel.class);
        when(emailChannel.id()).thenReturn("email");

        listener = new SubscriptionConfirmationListener(newsFetcher, List.of(emailChannel));
    }

    private static NewsItem item(String id, String title, List<String> tickers) {
        return item(id, title, tickers, Instant.EPOCH);
    }

    private static NewsItem item(String id, String title, List<String> tickers, Instant publishedAt) {
        return new NewsItem(
                title, "desc", URI.create("https://example.com/" + id), null,
                publishedAt, "Example", tickers, Map.of(),
                new NewsItem.ProviderRef("test", id), null
        );
    }

    private static Subscription subscription(String email, List<String> keywords) {
        var filter = new SubscriptionFilter();
        filter.setKeywords(keywords);
        filter.setTickers(List.of());
        filter.setLanguage(null);

        var sub = new Subscription();
        sub.setEmail(email);
        sub.setMaxItems(10);
        sub.setFilter(filter);
        sub.setEnabled(true);
        sub.setSchedule(SchedulePreset.MORNING);
        return sub;
    }

    @Nested
    @DisplayName("When the subscription has no email")
    class NoEmail {

        @Test
        @DisplayName("Does nothing")
        void doesNothing() {
            listener.onSubscriptionCreated(new SubscriptionCreatedEvent(subscription(null, List.of("tesla"))));

            verifyNoInteractions(newsFetcher, emailChannel);
        }
    }

    @Nested
    @DisplayName("When no email channel is registered")
    class NoEmailChannel {

        @Test
        @DisplayName("Does nothing and does not throw")
        void doesNothingAndDoesNotThrow() {
            var listenerWithoutEmailChannel = new SubscriptionConfirmationListener(newsFetcher, List.of());

            assertDoesNotThrow(() -> listenerWithoutEmailChannel.onSubscriptionCreated(
                    new SubscriptionCreatedEvent(subscription("user@example.com", List.of("tesla")))));

            verifyNoInteractions(newsFetcher);
        }
    }

    @Nested
    @DisplayName("When sending the confirmation")
    class SendingConfirmation {

        @Test
        @DisplayName("Includes currently matching news in the confirmation")
        void includesMatchingNews() {
            when(newsFetcher.fetchAll()).thenReturn(List.of(item("1", "Tesla rallies", List.of())));

            listener.onSubscriptionCreated(
                    new SubscriptionCreatedEvent(subscription("user@example.com", List.of("tesla"))));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(emailChannel).send(eq("user@example.com"), captor.capture());
            assertTrue(captor.getValue().body().contains("Tesla rallies"));
        }

        @Test
        @DisplayName("Falls back to a sample of unrelated news when nothing matches the filter")
        void fallsBackToSampleWhenNothingMatches() {
            when(newsFetcher.fetchAll()).thenReturn(List.of(item("1", "Unrelated weather report", List.of())));

            listener.onSubscriptionCreated(
                    new SubscriptionCreatedEvent(subscription("user@example.com", List.of("tesla"))));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(emailChannel).send(eq("user@example.com"), captor.capture());
            assertTrue(captor.getValue().body().contains("Unrelated weather report"));
            assertTrue(captor.getValue().body().toLowerCase().contains("nothing matches your filter"));
        }

        @Test
        @DisplayName("Caps the fallback sample at 5 items, most recent first")
        void fallbackSampleIsCappedAtFiveMostRecent() {
            Instant now = Instant.now();
            List<NewsItem> unrelatedItems = IntStream.range(0, 7)
                    .mapToObj(i -> item(String.valueOf(i), "Unrelated item " + i, List.of(), now.minusSeconds(i)))
                    .toList();
            when(newsFetcher.fetchAll()).thenReturn(unrelatedItems);

            listener.onSubscriptionCreated(
                    new SubscriptionCreatedEvent(subscription("user@example.com", List.of("tesla"))));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(emailChannel).send(eq("user@example.com"), captor.capture());
            String body = captor.getValue().body();
            assertTrue(body.contains("Unrelated item 0"));
            assertTrue(body.contains("Unrelated item 4"));
            assertFalse(body.contains("Unrelated item 5"));
            assertFalse(body.contains("Unrelated item 6"));
        }

        @Test
        @DisplayName("Sends a 'no news at all' message when nothing was fetched")
        void sendsNoNewsMessageWhenFetchIsEmpty() {
            when(newsFetcher.fetchAll()).thenReturn(List.of());

            listener.onSubscriptionCreated(
                    new SubscriptionCreatedEvent(subscription("user@example.com", List.of("tesla"))));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(emailChannel).send(eq("user@example.com"), captor.capture());
            assertTrue(captor.getValue().body().toLowerCase().contains("no news could be fetched"));
        }
    }

    @Nested
    @DisplayName("Fault isolation")
    class FaultIsolation {

        @Test
        @DisplayName("Does not propagate an exception from the news fetch")
        void doesNotPropagateFetchFailure() {
            when(newsFetcher.fetchAll()).thenThrow(new RuntimeException("Fetch failed"));

            assertDoesNotThrow(() -> listener.onSubscriptionCreated(
                    new SubscriptionCreatedEvent(subscription("user@example.com", List.of("tesla")))));
        }
    }
}
