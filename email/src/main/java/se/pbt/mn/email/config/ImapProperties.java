package se.pbt.mn.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for polling an IMAP inbox for inbound subscription emails.
 * <p>
 * Values are loaded from the application's configuration (module: {@code app-runner})
 * using the prefix {@code email.imap}.
 */
@Configuration
@ConfigurationProperties(prefix = "email.imap")
@Getter
@Setter
public class ImapProperties {

    /**
     * Whether the IMAP listener should run at all; off by default since it requires a
     * real mailbox to be configured.
     */
    private boolean enabled = false;

    private String host;
    private int port = 993;
    private String username;
    private String password;

    /**
     * Whether to connect over IMAPS (true, the normal case for real providers like Gmail)
     * or plain IMAP (false, used against local/test servers without TLS).
     */
    private boolean ssl = true;

    private String folder = "INBOX";

    private int pollIntervalSeconds = 60;
}
