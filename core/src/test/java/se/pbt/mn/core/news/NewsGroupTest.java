package se.pbt.mn.core.news;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NewsGroup")
class NewsGroupTest {

    private static NewsItem itemPublishedAt(String id, Instant publishedAt) {
        return new NewsItem(
                "title-" + id, null, null, null, publishedAt, null, null, null,
                new NewsItem.ProviderRef("test", id), null
        );
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Throws when items is null")
        void throwsOnNullItems() {
            assertThrows(IllegalArgumentException.class, () -> new NewsGroup(null));
        }

        @Test
        @DisplayName("Throws when items is empty")
        void throwsOnEmptyItems() {
            assertThrows(IllegalArgumentException.class, () -> new NewsGroup(List.of()));
        }

        @Test
        @DisplayName("Defensively copies the input list, into an unmodifiable one")
        void defensivelyCopiesItems() {
            var mutableInput = new ArrayList<>(List.of(itemPublishedAt("1", Instant.now())));
            var group = new NewsGroup(mutableInput);

            mutableInput.add(itemPublishedAt("2", Instant.now()));

            assertEquals(1, group.items().size());
            assertThrows(UnsupportedOperationException.class, () -> group.items().add(itemPublishedAt("3", Instant.now())));
        }
    }

    @Nested
    @DisplayName("primary()")
    class Primary {

        @Test
        @DisplayName("Returns the only item in a single-item group")
        void returnsOnlyItemInSingleItemGroup() {
            var item = itemPublishedAt("1", Instant.now());
            var group = new NewsGroup(List.of(item));

            assertEquals(item, group.primary());
        }

        @Test
        @DisplayName("Returns the earliest-published item among several")
        void returnsEarliestPublishedItem() {
            var earliest = itemPublishedAt("earliest", Instant.parse("2026-01-01T08:00:00Z"));
            var later = itemPublishedAt("later", Instant.parse("2026-01-01T09:00:00Z"));
            var group = new NewsGroup(List.of(later, earliest));

            assertEquals(earliest, group.primary());
        }

        @Test
        @DisplayName("Treats a null publishedAt as later than any real timestamp")
        void treatsNullPublishedAtAsLatest() {
            var withDate = itemPublishedAt("dated", Instant.parse("2026-01-01T08:00:00Z"));
            var withoutDate = itemPublishedAt("undated", null);
            var group = new NewsGroup(List.of(withoutDate, withDate));

            assertEquals(withDate, group.primary());
        }
    }
}
