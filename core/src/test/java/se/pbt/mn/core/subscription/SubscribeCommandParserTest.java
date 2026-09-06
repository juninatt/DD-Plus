package se.pbt.mn.core.subscription;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscribeCommandParser")
class SubscribeCommandParserTest {

    @Test
    @DisplayName("Parses a basic command with a leading slash (Telegram-style)")
    void parsesWithLeadingSlash() {
        var result = SubscribeCommandParser.parse("/subscribe Tesla en 10");

        assertEquals(List.of("Tesla"), result.keywords());
        assertEquals("en", result.language());
        assertEquals(10, result.maxItems());
    }

    @Test
    @DisplayName("Parses a basic command without a leading slash (email-style)")
    void parsesWithoutLeadingSlash() {
        var result = SubscribeCommandParser.parse("subscribe Tesla en 10");

        assertEquals(List.of("Tesla"), result.keywords());
        assertEquals("en", result.language());
        assertEquals(10, result.maxItems());
    }

    @Test
    @DisplayName("Is case-insensitive on the command token")
    void isCaseInsensitiveOnCommandToken() {
        var result = SubscribeCommandParser.parse("SUBSCRIBE Tesla en 10");

        assertEquals(List.of("Tesla"), result.keywords());
    }

    @Test
    @DisplayName("Throws when the command token is neither subscribe nor /subscribe")
    void throwsOnUnknownCommandToken() {
        assertThrows(IllegalArgumentException.class,
                () -> SubscribeCommandParser.parse("unsubscribe Tesla en 10"));
    }

    @Nested
    @DisplayName("Optional trailing email")
    class Email {

        @Test
        @DisplayName("Parses a trailing email address")
        void parsesTrailingEmail() {
            var result = SubscribeCommandParser.parse("subscribe Tesla en 10 user@example.com");

            assertEquals("user@example.com", result.email());
            assertEquals(10, result.maxItems());
        }

        @Test
        @DisplayName("Leaves email null when absent")
        void leavesEmailNullWhenAbsent() {
            var result = SubscribeCommandParser.parse("subscribe Tesla en 10");

            assertNull(result.email());
        }
    }
}
