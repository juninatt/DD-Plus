package se.pbt.ddplus.notifier.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Holds configuration values for the Telegram notifier.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private boolean enabled = true;
    private String botToken;
    private List<Long> chatIds = List.of();
    private String baseUrl;
}

