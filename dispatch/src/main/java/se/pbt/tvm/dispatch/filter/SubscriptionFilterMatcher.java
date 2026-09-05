package se.pbt.tvm.dispatch.filter;

import se.pbt.tvm.core.news.NewsItem;
import se.pbt.tvm.subscription.model.SubscriptionFilter;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matches a {@link NewsItem} against a {@link SubscriptionFilter}.
 * <p>
 * Each non-empty filter category (keywords, tickers, language) must match for the item to
 * be included; within a category, any one of multiple values matching is enough (OR).
 * Language is only enforced when the item declares one — most providers don't tag language,
 * and an unknown language shouldn't silently exclude an otherwise-matching item.
 */
public final class SubscriptionFilterMatcher {

    private SubscriptionFilterMatcher() {}

    public static boolean matches(NewsItem item, SubscriptionFilter filter) {
        return matchesKeywords(item, filter.getKeywords())
                && matchesTickers(item, filter.getTickers())
                && matchesLanguage(item, filter.getLanguage());
    }

    private static boolean matchesKeywords(NewsItem item, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        String haystack = (nullToEmpty(item.title()) + " " + nullToEmpty(item.description()))
                .toLowerCase(Locale.ROOT);

        return keywords.stream()
                .filter(Objects::nonNull)
                .map(k -> k.toLowerCase(Locale.ROOT))
                .anyMatch(haystack::contains);
    }

    private static boolean matchesTickers(NewsItem item, List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return true;
        }
        if (item.tickers() == null || item.tickers().isEmpty()) {
            return false;
        }

        Set<String> itemTickers = item.tickers().stream()
                .map(t -> t.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return tickers.stream()
                .filter(Objects::nonNull)
                .map(t -> t.toUpperCase(Locale.ROOT))
                .anyMatch(itemTickers::contains);
    }

    private static boolean matchesLanguage(NewsItem item, String language) {
        if (language == null || language.isBlank()) {
            return true;
        }
        if (item.language() == null || item.language().isBlank()) {
            return true;
        }
        return language.equalsIgnoreCase(item.language());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
