package se.pbt.mn.subscription.event;

import se.pbt.mn.subscription.model.Subscription;

/**
 * Published after a {@link Subscription} is successfully saved, so interested modules can
 * react (e.g. an immediate delivery confirmation) without {@code SubscriptionService}
 * needing to know about them.
 */
public record SubscriptionCreatedEvent(Subscription subscription) {}
