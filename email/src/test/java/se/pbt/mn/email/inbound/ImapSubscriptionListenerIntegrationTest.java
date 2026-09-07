package se.pbt.mn.email.inbound;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.mn.email.config.ImapProperties;
import se.pbt.mn.subscription.mapper.SubscribeCommandMapper;
import se.pbt.mn.subscription.model.Subscription;
import se.pbt.mn.subscription.service.SubscriptionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises a real (in-memory) IMAP round trip -- a message delivered into a mailbox is
 * found, processed, and marked read so it isn't reprocessed on the next poll.
 */
@DisplayName("ImapSubscriptionListener (against a real IMAP server)")
class ImapSubscriptionListenerIntegrationTest {

    private static final String MAILBOX = "subscribe@localhost";
    private static final String PASSWORD = "test-password";

    private GreenMail greenMail;
    private SubscriptionService subscriptionService;
    private ImapSubscriptionListener listener;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        greenMail.start();
        greenMail.setUser(MAILBOX, PASSWORD);

        var properties = new ImapProperties();
        properties.setHost("localhost");
        properties.setPort(greenMail.getImap().getServerSetup().getPort());
        properties.setUsername(MAILBOX);
        properties.setPassword(PASSWORD);
        properties.setSsl(false);
        properties.setFolder("INBOX");

        subscriptionService = mock(SubscriptionService.class);
        when(subscriptionService.save(any())).thenReturn(SubscriptionService.SaveResult.ok("saved"));
        listener = new ImapSubscriptionListener(properties, subscriptionService, new SubscribeCommandMapper());
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    @DisplayName("Processes an unseen message and does not reprocess it on the next poll")
    void processesUnseenMessageOnce() throws Exception {
        GreenMailUtil.sendTextEmail(
                MAILBOX, "sender@example.com", "Subscribe", "subscribe Tesla en 10",
                greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(1);

        listener.pollOnce();
        verify(subscriptionService, times(1)).save(any(Subscription.class));

        listener.pollOnce();
        verify(subscriptionService, times(1)).save(any(Subscription.class));
    }
}
