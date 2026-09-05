package se.pbt.tvm.core.news;

import java.util.List;

/**
 * A provider of financial news articles, normalized into {@link NewsItem}s.
 * <p>
 * Each implementation is responsible for fetching from its own external API and
 * mapping the response into the shared domain model. Implementations should not
 * throw on fetch/parse failure — they should log and return an empty list, so one
 * failing provider doesn't prevent others from being dispatched.
 */
public interface NewsSource {

    /**
     * Short, stable identifier for this provider (e.g. "finnhub", "marketaux").
     */
    String id();

    /**
     * Fetches the latest available news items from this provider.
     */
    List<NewsItem> fetchLatest();
}
