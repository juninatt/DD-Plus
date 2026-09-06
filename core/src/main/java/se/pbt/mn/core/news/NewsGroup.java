package se.pbt.mn.core.news;

import java.util.Comparator;
import java.util.List;

/**
 * A cluster of one or more {@link NewsItem}s considered to represent the same underlying
 * event -- e.g. multiple outlets covering the same earnings report. A standalone article
 * with nothing else grouped to it is simply a group of one.
 */
public record NewsGroup(List<NewsItem> items) {

    public NewsGroup {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("NewsGroup must contain at least one item");
        }
        items = List.copyOf(items);
    }

    /**
     * The earliest-published item in the group -- the first outlet to report the story.
     */
    public NewsItem primary() {
        return items.stream()
                .min(Comparator.comparing(NewsItem::publishedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow();
    }
}
