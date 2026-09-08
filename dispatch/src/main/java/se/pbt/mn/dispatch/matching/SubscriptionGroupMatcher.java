package se.pbt.mn.dispatch.matching;

import se.pbt.mn.core.news.NewsGroup;
import se.pbt.mn.dispatch.filter.SubscriptionFilterMatcher;
import se.pbt.mn.subscription.model.Subscription;

import java.util.List;

/**
 * Selects the {@link NewsGroup}s that match a subscription's filter, truncated to its
 * {@code maxItems}. A group matches if any item in it does -- the whole group travels
 * together so a subscriber sees every outlet covering a story they're subscribed to.
 */
public final class SubscriptionGroupMatcher {

    private SubscriptionGroupMatcher() {}

    public static List<NewsGroup> match(List<NewsGroup> groups, Subscription subscription) {
        return groups.stream()
                .filter(group -> group.items().stream()
                        .anyMatch(item -> SubscriptionFilterMatcher.matches(item, subscription.getFilter())))
                .limit(Math.max(subscription.getMaxItems(), 0))
                .toList();
    }
}
