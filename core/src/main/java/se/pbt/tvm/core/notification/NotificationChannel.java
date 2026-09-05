package se.pbt.tvm.core.notification;

/**
 * A delivery channel capable of sending a {@link Notification} to a recipient.
 * <p>
 * {@code recipientId} is currently Telegram-chat-id shaped, since Telegram is
 * the only implemented channel today. A future channel addressed differently
 * (e.g. email) will need a broader recipient concept on the subscription
 * domain model — this interface only decouples *how* a message is delivered,
 * not yet *who* it's addressed to.
 */
public interface NotificationChannel {

    /**
     * Short, stable identifier for this channel (e.g. "telegram").
     */
    String id();

    /**
     * Sends the given notification to the given recipient via this channel.
     */
    void send(long recipientId, Notification notification);
}
