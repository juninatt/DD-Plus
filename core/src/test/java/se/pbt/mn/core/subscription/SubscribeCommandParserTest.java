package se.pbt.mn.core.subscription;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscribeCommandParser")
class SubscribeCommandParserTest {

    @Nested
    @DisplayName("Command structure")
    class CommandStructure {

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

        @Test
        @DisplayName("Throws on too few arguments")
        void throwsOnTooFewArguments() {
            assertThrows(IllegalArgumentException.class,
                    () -> SubscribeCommandParser.parse("/subscribe en 5"));
        }

        @Test
        @DisplayName("Accepts leading/trailing/multiple spaces")
        void acceptsExtraWhitespace() {
            var result = SubscribeCommandParser.parse("   /subscribe   AI   en   5   ");

            assertEquals(List.of("AI"), result.keywords());
            assertEquals("en", result.language());
            assertEquals(5, result.maxItems());
        }
    }

    @Nested
    @DisplayName("Language token")
    class Language {

        @Test
        @DisplayName("Throws if language is not 2 letters")
        void failsOnInvalidLanguage() {
            assertThrows(IllegalArgumentException.class,
                    () -> SubscribeCommandParser.parse("/subscribe Tesla english 5"));
        }
    }

    @Nested
    @DisplayName("Keywords")
    class Keywords {

        @Test
        @DisplayName("Parses a single quoted multi-word keyword")
        void parsesQuotedMultiWordKeyword() {
            var result = SubscribeCommandParser.parse("/subscribe \"Silicon Valley\" sv 3");

            assertEquals(List.of("Silicon Valley"), result.keywords());
            assertEquals("sv", result.language());
            assertEquals(3, result.maxItems());
        }

        @Test
        @DisplayName("Supports non-ASCII characters in a quoted keyword")
        void parsesQuotedKeywordWithNonAsciiCharacters() {
            var result = SubscribeCommandParser.parse("/subscribe \"Skåne Mejerier\" sv 5");

            assertEquals(List.of("Skåne Mejerier"), result.keywords());
        }

        @Test
        @DisplayName("Parses multiple quoted multi-word keywords")
        void parsesSeveralQuotedKeywords() {
            var result = SubscribeCommandParser.parse(
                    "/subscribe \"VanEck Space Innovations UCITS ETF\" \"Meta Space Fund\" \"Silicon Valley\" sv 3");

            assertEquals(
                    List.of("VanEck Space Innovations UCITS ETF", "Meta Space Fund", "Silicon Valley"),
                    result.keywords()
            );
        }

        @Test
        @DisplayName("Parses mixed quoted and unquoted keywords")
        void parsesMixedQuotedAndUnquotedKeywords() {
            var result = SubscribeCommandParser.parse("/subscribe Tesla \"Silicon Valley\" en 2");

            assertEquals(List.of("Tesla", "Silicon Valley"), result.keywords());
        }

        @Test
        @DisplayName("Treats a hyphenated word as a single keyword")
        void treatsHyphenatedWordAsSingleKeyword() {
            var result = SubscribeCommandParser.parse("/subscribe Tesla Silicon-Valley en 2");

            assertEquals(List.of("Tesla", "Silicon-Valley"), result.keywords());
        }

        @Test
        @DisplayName("Throws if no keywords are given")
        void failsIfNoKeywordsGiven() {
            assertThrows(IllegalArgumentException.class,
                    () -> SubscribeCommandParser.parse("/subscribe en 5"));
        }
    }

    @Nested
    @DisplayName("Max items")
    class MaxItems {

        @Test
        @DisplayName("Throws on non-numeric maxItems")
        void failsOnNonNumericMaxItems() {
            Exception e = assertThrows(IllegalArgumentException.class,
                    () -> SubscribeCommandParser.parse("/subscribe Tesla en notanumber"));

            assertTrue(e.getMessage().contains("maxItems"));
        }
    }

    @Nested
    @DisplayName("Schedule token")
    class Schedule {

        @Test
        @DisplayName("Parses a multi-letter alias ('me' for morning_evening)")
        void parsesMultiLetterScheduleAlias() {
            var result = SubscribeCommandParser.parse("/subscribe AI en me 10");

            assertEquals(SchedulePreset.MORNING_EVENING, result.schedule());
            assertEquals(List.of("AI"), result.keywords());
            assertEquals("en", result.language());
        }

        @Test
        @DisplayName("Parses a single-letter alias ('m' for morning)")
        void parsesSingleLetterScheduleAlias() {
            var result = SubscribeCommandParser.parse("/subscribe Tesla en m 5");

            assertEquals(SchedulePreset.MORNING, result.schedule());
            assertEquals("en", result.language());
            assertEquals(5, result.maxItems());
        }

        @Test
        @DisplayName("Parses a full preset name ('evening')")
        void parsesFullScheduleName() {
            var result = SubscribeCommandParser.parse("/subscribe \"Silicon Valley\" en evening 2");

            assertEquals(SchedulePreset.EVENING, result.schedule());
            assertEquals(List.of("Silicon Valley"), result.keywords());
            assertEquals("en", result.language());
        }

        @Test
        @DisplayName("Leaves schedule null when absent, for the caller to apply a default")
        void leavesScheduleNullWhenAbsent() {
            var result = SubscribeCommandParser.parse("/subscribe AI en 5");

            assertNull(result.schedule());
            assertEquals("en", result.language());
            assertEquals(List.of("AI"), result.keywords());
        }

        @Test
        @DisplayName("An unrecognized alias falls through to language validation and fails")
        void unknownAliasFailsAsInvalidLanguage() {
            Exception e = assertThrows(IllegalArgumentException.class,
                    () -> SubscribeCommandParser.parse("/subscribe AI en nonsense 5"));

            assertTrue(e.getMessage().toLowerCase().contains("language"));
        }

        @Test
        @DisplayName("A schedule token placed after maxItems is not recognized")
        void scheduleAfterMaxItemsIsNotRecognized() {
            assertThrows(IllegalArgumentException.class,
                    () -> SubscribeCommandParser.parse("/subscribe AI en 5 me"));
        }
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
        @DisplayName("Parses a trailing email address alongside an explicit schedule")
        void parsesTrailingEmailWithSchedule() {
            var result = SubscribeCommandParser.parse("subscribe Tesla en morning 10 user@example.com");

            assertEquals("user@example.com", result.email());
            assertEquals(SchedulePreset.MORNING, result.schedule());
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
