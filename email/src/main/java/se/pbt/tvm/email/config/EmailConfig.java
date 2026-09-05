package se.pbt.tvm.email.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for setting up a WebClient bean to interact with the Resend API.
 * <p>
 * This bean is automatically injected with values from {@link EmailApiProperties},
 * which are populated from the application’s configuration.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EmailApiProperties.class)
public class EmailConfig {

    /**
     * Creates and configures the {@code resendClient} bean used for making HTTP requests to Resend.
     */
    @Bean("resendClient")
    public WebClient resendWebClient(EmailApiProperties properties) {
        log.debug("Creating WebClient for Resend with base URL: {}", properties.getBaseUrl());

        WebClient client = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .build();

        log.info("Resend WebClient bean successfully created");
        return client;
    }
}
