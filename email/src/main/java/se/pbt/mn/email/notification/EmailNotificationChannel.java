package se.pbt.mn.email.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.pbt.mn.core.notification.Notification;
import se.pbt.mn.core.notification.NotificationChannel;
import se.pbt.mn.email.config.EmailApiProperties;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * {@link NotificationChannel} implementation that delivers notifications as emails via
 * the Resend API (https://resend.com).
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private static final String SEND_PATH = "/emails";
    private static final DateTimeFormatter PUBLISHED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final WebClient client;
    private final EmailApiProperties properties;

    public EmailNotificationChannel(@Qualifier("resendClient") WebClient client, EmailApiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String id() {
        return "email";
    }

    @Override
    public void send(String recipientAddress, Notification notification) {
        if (recipientAddress == null || recipientAddress.isBlank()) {
            return;
        }

        var request = new ResendEmailRequest(
                properties.getFromAddress(),
                List.of(recipientAddress),
                subject(notification),
                toHtml(notification)
        );

        try {
            client.post()
                    .uri(SEND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new RuntimeException(
                                            "Resend API send failed: status=" + resp.statusCode() + ", body=" + body)))
                    )
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("Failed to send email notification to {}", recipientAddress, e);
        }
    }

    private String subject(Notification notification) {
        String title = notification.title();
        return "📰 " + (title == null || title.isBlank() ? "News update" : title);
    }

    private String toHtml(Notification notification) {
        StringBuilder html = new StringBuilder();

        if (hasText(notification.title())) {
            html.append("<h2>").append(escapeHtml(notification.title())).append("</h2>");
        }
        if (hasText(notification.body())) {
            html.append("<p>").append(escapeHtml(notification.body()).replace("\n", "<br>")).append("</p>");
        }

        String meta = formatMeta(notification);
        if (meta != null) {
            html.append("<p>").append(meta).append("</p>");
        }

        if (notification.tickers() != null && !notification.tickers().isEmpty()) {
            String tags = notification.tickers().stream()
                    .map(EmailNotificationChannel::escapeHtml)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            html.append("<p><strong>Tickers:</strong> ").append(tags).append("</p>");
        }

        if (hasText(notification.url())) {
            html.append("<p><a href=\"").append(escapeHtml(notification.url())).append("\">Läs mer</a></p>");
        }

        return html.toString();
    }

    private String formatMeta(Notification notification) {
        String source = hasText(notification.source()) ? escapeHtml(notification.source()) : null;
        String published = notification.publishedAt() != null
                ? escapeHtml(PUBLISHED_AT_FORMAT.format(notification.publishedAt()))
                : null;

        if (source != null && published != null) {
            return "<strong>" + source + "</strong> · " + published;
        }
        return source != null ? "<strong>" + source + "</strong>" : published;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String html) {}
}
