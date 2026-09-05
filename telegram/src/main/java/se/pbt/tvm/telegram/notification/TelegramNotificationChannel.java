package se.pbt.tvm.telegram.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.pbt.tvm.core.notification.Notification;
import se.pbt.tvm.core.notification.NotificationChannel;
import se.pbt.tvm.telegram.client.TelegramApiClient;

/**
 * {@link NotificationChannel} implementation that delivers notifications as
 * Telegram messages via the existing {@link TelegramApiClient}.
 */
@Component
public class TelegramNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationChannel.class);

    private final TelegramApiClient apiClient;

    public TelegramNotificationChannel(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String id() {
        return "telegram";
    }

    @Override
    public void send(long recipientId, Notification notification) {
        try {
            apiClient.sendMessage(recipientId, format(notification)).block();
        } catch (Exception e) {
            log.error("Failed to send Telegram notification to chatId={}", recipientId, e);
        }
    }

    private String format(Notification notification) {
        StringBuilder sb = new StringBuilder();
        if (notification.title() != null) {
            sb.append(notification.title()).append("\n");
        }
        if (notification.body() != null) {
            sb.append(notification.body());
        }
        if (notification.url() != null) {
            sb.append("\n").append(notification.url());
        }
        return sb.toString().trim();
    }
}
