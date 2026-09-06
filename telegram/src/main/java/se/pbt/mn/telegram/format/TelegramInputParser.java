package se.pbt.mn.telegram.format;

import org.springframework.stereotype.Component;
import se.pbt.mn.core.subscription.ParsedSubscribeCommand;
import se.pbt.mn.core.subscription.SubscribeCommand;
import se.pbt.mn.core.subscription.SubscribeCommandParser;
import se.pbt.mn.telegram.model.TelegramCommand;

/**
 * Responsible for interpreting raw Telegram input and converting it into structured command objects.
 * <p>
 * Delegates the actual "subscribe" syntax parsing to {@link SubscribeCommandParser}, which is
 * shared with other channels (e.g. inbound subscription emails), and attaches the
 * Telegram-specific chat id on top.
 */
@Component
public class TelegramInputParser {

    /**
     * Parses a /subscribe command into a {@link SubscribeCommand}.
     * See {@link SubscribeCommandParser#parse(String)} for the accepted syntax.
     */
    public SubscribeCommand parseSubscribeCommand(TelegramCommand command) {
        ParsedSubscribeCommand parsed = SubscribeCommandParser.parse(command.message());
        return new SubscribeCommand(
                command.chatId(),
                parsed.language(),
                parsed.maxItems(),
                parsed.keywords(),
                parsed.schedule(),
                parsed.email()
        );
    }
}
