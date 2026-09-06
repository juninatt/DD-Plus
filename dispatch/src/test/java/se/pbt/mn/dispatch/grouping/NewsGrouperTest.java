package se.pbt.mn.dispatch.grouping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.core.news.NewsItem;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NewsGrouper")
class NewsGrouperTest {

    private static NewsItem item(String id, String source, List<String> tickers, Instant publishedAt) {
        return new NewsItem(
                "Title " + id, "desc", URI.create("https://example.com/" + id), null,
                publishedAt, source, tickers, Map.of(),
                new NewsItem.ProviderRef("test", id), null
        );
    }

    @Test
    @DisplayName("Returns an empty list for an empty input")
    void group_withEmptyInput_returnsEmptyList() {
        assertTrue(NewsGrouper.group(List.of()).isEmpty());
    }

    @Test
    @DisplayName("A single item forms its own group")
    void group_withSingleItem_formsSingleGroup() {
        var item = item("1", "Reuters", List.of("AAPL"), Instant.now());

        List<NewsGroup> groups = NewsGrouper.group(List.of(item));

        assertEquals(1, groups.size());
        assertEquals(1, groups.get(0).items().size());
    }

    @Nested
    @DisplayName("Shared-ticker + time-window matching")
    class SharedTickerMatching {

        @Test
        @DisplayName("Groups two items sharing a ticker within the time window")
        void group_withSharedTickerWithinWindow_groupsTogether() {
            Instant now = Instant.now();
            var a = item("1", "Reuters", List.of("AAPL"), now);
            var b = item("2", "MarketWatch", List.of("AAPL"), now.plus(2, ChronoUnit.HOURS));

            List<NewsGroup> groups = NewsGrouper.group(List.of(a, b));

            assertEquals(1, groups.size());
            assertEquals(2, groups.get(0).items().size());
        }

        @Test
        @DisplayName("Does not group items sharing a ticker but outside the time window")
        void group_withSharedTickerOutsideWindow_staysSeparate() {
            Instant now = Instant.now();
            var a = item("1", "Reuters", List.of("AAPL"), now);
            var b = item("2", "MarketWatch", List.of("AAPL"), now.plus(3, ChronoUnit.DAYS));

            List<NewsGroup> groups = NewsGrouper.group(List.of(a, b));

            assertEquals(2, groups.size());
        }

        @Test
        @DisplayName("Does not group items with no overlapping tickers")
        void group_withDifferentTickers_staysSeparate() {
            Instant now = Instant.now();
            var a = item("1", "Reuters", List.of("AAPL"), now);
            var b = item("2", "MarketWatch", List.of("TSLA"), now);

            List<NewsGroup> groups = NewsGrouper.group(List.of(a, b));

            assertEquals(2, groups.size());
        }

        @Test
        @DisplayName("Never groups items with no tickers at all, even when published at the same instant")
        void group_withNoTickers_staysSeparate() {
            Instant now = Instant.now();
            var a = item("1", "Reuters", List.of(), now);
            var b = item("2", "MarketWatch", List.of(), now);

            List<NewsGroup> groups = NewsGrouper.group(List.of(a, b));

            assertEquals(2, groups.size());
        }

        @Test
        @DisplayName("Does not group when publishedAt is missing on either item")
        void group_withMissingPublishedAt_staysSeparate() {
            var a = item("1", "Reuters", List.of("AAPL"), null);
            var b = item("2", "MarketWatch", List.of("AAPL"), Instant.now());

            List<NewsGroup> groups = NewsGrouper.group(List.of(a, b));

            assertEquals(2, groups.size());
        }
    }

    @Test
    @DisplayName("Grouping is transitive through a bridging item")
    void group_withTransitiveBridge_mergesAllThree() {
        Instant now = Instant.now();
        var a = item("1", "Reuters", List.of("TSLA"), now);
        var b = item("2", "MarketWatch", List.of("TSLA", "AAPL"), now.plus(1, ChronoUnit.HOURS));
        var c = item("3", "Bloomberg", List.of("AAPL"), now.plus(2, ChronoUnit.HOURS));

        List<NewsGroup> groups = NewsGrouper.group(List.of(a, b, c));

        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).items().size());
    }

    @Test
    @DisplayName("NewsGroup.primary() returns the earliest-published item")
    void primary_returnsEarliestPublishedItem() {
        Instant now = Instant.now();
        var earlier = item("1", "Reuters", List.of("AAPL"), now);
        var later = item("2", "MarketWatch", List.of("AAPL"), now.plus(1, ChronoUnit.HOURS));

        List<NewsGroup> groups = NewsGrouper.group(List.of(later, earlier));

        assertEquals("1", groups.get(0).primary().providerRef().id());
    }
}
