package se.pbt.mn.email.inbound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.pbt.mn.email.config.ImapProperties;
import se.pbt.mn.subscription.mapper.SubscribeCommandMapper;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.service.SubscriptionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ImapSubscriptionListener")
class ImapSubscriptionListenerTest {

    private SubscriptionService subscriptionService;
    private ImapSubscriptionListener listener;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        listener = new ImapSubscriptionListener(new ImapProperties(), subscriptionService, new SubscribeCommandMapper());
    }

    @Test
    @DisplayName("Uses the sender's address when the body has no trailing email")
    void usesSenderAddressWhenBodyHasNoEmail() {
        listener.processMessage("sender@example.com", "subscribe Tesla en 10");

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionService).save(captor.capture());
        assertEquals("sender@example.com", captor.getValue().getEmail());
        assertEquals(0, captor.getValue().getChatId());
        assertEquals(List.of("Tesla"), captor.getValue().getFilter().getKeywords());
    }

    @Test
    @DisplayName("Accepts the command without a leading slash")
    void acceptsCommandWithoutLeadingSlash() {
        listener.processMessage("sender@example.com", "subscribe \"AI stocks\" en morning 5");

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionService).save(captor.capture());
        assertEquals(List.of("AI stocks"), captor.getValue().getFilter().getKeywords());
        assertEquals(5, captor.getValue().getMaxItems());
    }

    @Test
    @DisplayName("Prefers an explicit trailing email in the body over the sender's address")
    void prefersExplicitEmailOverSender() {
        listener.processMessage("sender@example.com", "subscribe Tesla en 10 other@example.com");

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionService).save(captor.capture());
        assertEquals("other@example.com", captor.getValue().getEmail());
    }

    @Test
    @DisplayName("Does not save a subscription when the body is malformed")
    void doesNotSaveWhenBodyIsMalformed() {
        assertThrows(IllegalArgumentException.class,
                () -> listener.processMessage("sender@example.com", "this is not a command"));

        verifyNoInteractions(subscriptionService);
    }
}
