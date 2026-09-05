package se.pbt.tvm.telegram.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import se.pbt.tvm.core.notification.Notification;
import se.pbt.tvm.telegram.client.TelegramApiClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TelegramNotificationChannel")
class TelegramNotificationChannelTest {

    private static final long CHAT_ID = 123L;

    private TelegramApiClient apiClient;
    private TelegramNotificationChannel channel;

    @BeforeEach
    void setUp() {
        apiClient = mock(TelegramApiClient.class);
        channel = new TelegramNotificationChannel(apiClient);
    }

    @Test
    @DisplayName("id() returns 'telegram'")
    void id_returnsTelegram() {
        assertEquals("telegram", channel.id());
    }

    @Test
    @DisplayName("Sends a message combining title, body and url")
    void send_withFullNotification_sendsCombinedMessage() {
        when(apiClient.sendMessage(anyLong(), anyString())).thenReturn(Mono.empty());

        channel.send(CHAT_ID, new Notification("Title", "Body text", "https://example.com"));

        verify(apiClient).sendMessage(CHAT_ID, "Title\nBody text\nhttps://example.com");
    }

    @Test
    @DisplayName("When the API call fails, the exception does not propagate")
    void send_withApiFailure_doesNotPropagateException() {
        when(apiClient.sendMessage(anyLong(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("API down")));

        assertDoesNotThrow(() -> channel.send(CHAT_ID, new Notification("Title", "Body", null)));
    }
}
