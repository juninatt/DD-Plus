package se.pbt.tvm.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Resend WebClient.
 * <p>
 * Values are loaded from the application's configuration (module: {@code app-runner})
 * using the prefix {@code email.api}.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "email.api")
public class EmailApiProperties {

    /**
     * Base URL for the Resend API.
     */
    private String baseUrl;

    /**
     * API key for authenticating with Resend.
     */
    private String token;

    /**
     * Verified sender address emails are sent from (e.g. news@yourdomain.com), or
     * Resend's shared test sender if no domain has been verified yet.
     */
    private String fromAddress;
}
