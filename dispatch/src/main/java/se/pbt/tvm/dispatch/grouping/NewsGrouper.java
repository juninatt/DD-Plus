package se.pbt.tvm.dispatch.grouping;

import se.pbt.tvm.core.news.NewsGroup;
import se.pbt.tvm.core.news.NewsItem;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clusters {@link NewsItem}s that likely cover the same underlying event into
 * {@link NewsGroup}s.
 * <p>
 * Two items are considered the same event if they share at least one ticker and were
 * published within {@link #TIME_WINDOW} of each other. Items with no tickers at all (e.g.
 * general market news) never group with anything and each become their own group of one.
 * Grouping is transitive: if A pairs with B and B pairs with C, all three end up in the
 * same group even if A and C don't directly share a ticker.
 */
public final class NewsGrouper {

    private static final Duration TIME_WINDOW = Duration.ofHours(6);

    private NewsGrouper() {}

    public static List<NewsGroup> group(List<NewsItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        List<NewsItem> ordered = List.copyOf(items);
        int n = ordered.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (sameEvent(ordered.get(i), ordered.get(j))) {
                    union(parent, i, j);
                }
            }
        }

        Map<Integer, List<NewsItem>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            clusters.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(ordered.get(i));
        }

        return clusters.values().stream().map(NewsGroup::new).toList();
    }

    private static boolean sameEvent(NewsItem a, NewsItem b) {
        if (!shareTicker(a, b)) {
            return false;
        }
        if (a.publishedAt() == null || b.publishedAt() == null) {
            return false;
        }
        return Duration.between(a.publishedAt(), b.publishedAt()).abs().compareTo(TIME_WINDOW) <= 0;
    }

    private static boolean shareTicker(NewsItem a, NewsItem b) {
        if (a.tickers() == null || b.tickers() == null || a.tickers().isEmpty() || b.tickers().isEmpty()) {
            return false;
        }
        Set<String> bTickers = new HashSet<>(b.tickers());
        return a.tickers().stream().anyMatch(bTickers::contains);
    }

    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static void union(int[] parent, int a, int b) {
        int rootA = find(parent, a);
        int rootB = find(parent, b);
        if (rootA != rootB) {
            parent[rootA] = rootB;
        }
    }
}
