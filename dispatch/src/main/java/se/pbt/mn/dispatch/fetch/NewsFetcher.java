package se.pbt.mn.dispatch.fetch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.pbt.mn.core.news.NewsItem;
import se.pbt.mn.core.news.NewsSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches every registered {@link NewsSource}, isolating per-source failures, and dedupes
 * the combined result by {@link NewsItem.ProviderRef}.
 */
@Component
public class NewsFetcher {

    private static final Logger log = LoggerFactory.getLogger(NewsFetcher.class);

    private final List<NewsSource> sources;

    public NewsFetcher(List<NewsSource> sources) {
        this.sources = sources;
    }

    public List<NewsItem> fetchAll() {
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
}
