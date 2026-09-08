package se.pbt.mn.telegram.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.pbt.mn.telegram.model.TelegramCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers only what's specific to this class: attaching the Telegram chat id on top of
 * {@link se.pbt.mn.core.subscription.SubscribeCommandParser}. The parsing syntax itself
 * (schedule aliases, quoted keywords, error cases, ...) is covered where that logic lives,
 * in {@code SubscribeCommandParserTest} in the {@code core} module.
 */
@DisplayName("TelegramInputParser")
class TelegramInputParserTest {

    private final TelegramInputParser parser = new TelegramInputParser();

    @Test
    @DisplayName("Attaches the Telegram chat id to the parsed command")
    void attachesChatIdFromTelegramCommand() {
        var result = parser.parseSubscribeCommand(new TelegramCommand(123L, "/subscribe Tesla en 5"));

        assertEquals(123L, result.chatId());
        assertEquals(List.of("Tesla"), result.keywords());
        assertEquals("en", result.language());
        assertEquals(5, result.maxItems());
    }

    @Test
    @DisplayName("Propagates parsing failures instead of swallowing them")
    void propagatesParserExceptionsOnInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseSubscribeCommand(new TelegramCommand(1L, "/start AI en 5")));
    }
}
