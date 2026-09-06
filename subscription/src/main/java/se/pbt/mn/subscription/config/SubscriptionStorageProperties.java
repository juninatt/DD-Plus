package se.pbt.mn.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Holds configuration for the file-based subscription storage.
 * <p>
 * Maps the {@code subscription.storage} section in configuration, providing the single
 * file path used by {@link se.pbt.mn.subscription.persistence.SubscriptionStorage} to
 * load and save subscriptions for all callers (Telegram commands, scheduled dispatch, etc).
 */
@Configuration
@ConfigurationProperties(prefix = "subscription.storage")
@Getter
@Setter
public class SubscriptionStorageProperties {
    private String path = "subscriptions.yml";
}
