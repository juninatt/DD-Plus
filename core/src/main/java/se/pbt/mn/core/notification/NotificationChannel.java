package se.pbt.mn.core.notification;

/**
 * A delivery channel capable of sending a {@link Notification} to a recipient.
 * <p>
 * {@code recipientAddress} is a channel-agnostic string (a Telegram chat id in string
 * form, an email address, etc.) — the caller is responsible for resolving the correct
 * address for a given subscription and channel; a channel just knows how to deliver
 * to an address shaped for itself.
 */
public interface NotificationChannel {

    /**
     * Short, stable identifier for this channel (e.g. "telegram", "email").
     */
    String id();

    /**
     * Sends the given notification to the given recipient address via this channel.
     */
    void send(String recipientAddress, Notification notification);
}
