package se.pbt.mn.core.subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw "subscribe" request text into a {@link ParsedSubscribeCommand}, independent of
 * which channel the text arrived through (a Telegram /subscribe command, an inbound
 * subscription email, ...).
 * <p>
 * Focuses solely on syntactic interpretation and normalization, not business validation.
 */
public final class SubscribeCommandParser {

    // Matches either quoted strings or single non-whitespace tokens
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]+)\"|(\\S+)");

    // Permissive email check -- just enough to tell "this trailing token is an address"
    // apart from maxItems, not a full RFC 5322 validator.
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Map<String, SchedulePreset> SCHEDULE_ALIASES = Map.ofEntries(
            Map.entry("morning", SchedulePreset.MORNING),
            Map.entry("m", SchedulePreset.MORNING),
            Map.entry("evening", SchedulePreset.EVENING),
            Map.entry("e", SchedulePreset.EVENING),
            Map.entry("morning_evening", SchedulePreset.MORNING_EVENING),
            Map.entry("me", SchedulePreset.MORNING_EVENING),
            Map.entry("morning_lunch_evening", SchedulePreset.MORNING_LUNCH_EVENING),
            Map.entry("mle", SchedulePreset.MORNING_LUNCH_EVENING),
            Map.entry("europe_open", SchedulePreset.EUROPE_MARKET_OPEN),
            Map.entry("eo", SchedulePreset.EUROPE_MARKET_OPEN),
            Map.entry("europe_close", SchedulePreset.EUROPE_MARKET_CLOSE),
            Map.entry("ec", SchedulePreset.EUROPE_MARKET_CLOSE),
            Map.entry("us_open", SchedulePreset.US_MARKET_OPEN),
            Map.entry("uo", SchedulePreset.US_MARKET_OPEN),
            Map.entry("us_close", SchedulePreset.US_MARKET_CLOSE),
            Map.entry("uc", SchedulePreset.US_MARKET_CLOSE)
    );

    private SubscribeCommandParser() {}

    /**
     * Parses a subscribe request. Format:
     *   [/]subscribe <keywords...> <language> [schedule] <maxItems> [email]
     * The leading "/" is optional, so the same syntax works for a Telegram command and for
     * an inbound subscription email body. Where [schedule] is optional and can be one of:
     *   morning|m, evening|e, morning_evening|me, morning_lunch_evening|mle,
     *   europe_open|eo, europe_close|ec, us_open|uo, us_close|uc
     * Where [email] is optional -- if the last token looks like an email address, the
     * subscription is also delivered to that address in addition to Telegram.
     */
    public static ParsedSubscribeCommand parse(String rawMessage) {
        List<String> tokens = extractTokens(rawMessage.strip());
        String email = extractTrailingEmailOrNull(tokens);

        validateCommandFormat(tokens);

        int maxItems = extractAndValidateMaxItems(tokens.get(tokens.size() - 1));

        String maybeSchedule = tokens.get(tokens.size() - 2);
        SchedulePreset schedule = parseScheduleOrNull(maybeSchedule);

        final String language;
        final List<String> keywords;

        if (schedule != null) {
            language = extractAndValidateLanguage(tokens.get(tokens.size() - 3));
            keywords = extractKeywords(tokens, tokens.size() - 3);
        } else {
            language = extractAndValidateLanguage(maybeSchedule);
            keywords = extractKeywords(tokens, tokens.size() - 2);
        }

        return new ParsedSubscribeCommand(language, maxItems, keywords, schedule, email);
    }

    /**
     * Splits the incoming message into tokens.
     * <p>
     * Supports both quoted and unquoted segments, allowing
     * multi-word keyword phrases such as {@code "AI stocks"}.
     */
    private static List<String> extractTokens(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        while (matcher.find()) {
            tokens.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }
        return tokens;
    }

    /**
     * Ensures that the parsed token sequence starts with {@code subscribe} (with or without
     * a leading "/") and contains at least the required arguments.
     */
    private static void validateCommandFormat(List<String> tokens) {
        if (tokens.size() < 4 || !tokens.get(0).replaceFirst("^/", "").equalsIgnoreCase("subscribe")) {
            throw new IllegalArgumentException(
                    "Usage: subscribe <keywords> <language> [schedule] <maxItems> [email]");
        }
    }

    /**
     * Removes and returns the trailing token if it looks like an email address, leaving
     * the remaining tokens for the rest of the parser to handle as before.
     */
    private static String extractTrailingEmailOrNull(List<String> tokens) {
        if (tokens.isEmpty()) {
            return null;
        }
        String last = tokens.get(tokens.size() - 1);
        if (EMAIL_PATTERN.matcher(last).matches()) {
            tokens.remove(tokens.size() - 1);
            return last;
        }
        return null;
    }

    /**
     * Returns a SchedulePreset if token matches an alias; otherwise null.
     */
    private static SchedulePreset parseScheduleOrNull(String token) {
        if (token == null) return null;
        return SCHEDULE_ALIASES.get(token.toLowerCase(Locale.ROOT));
    }

    /**
     * Validates language token.
     */
    private static String extractAndValidateLanguage(String langToken) {
        if (langToken == null || !langToken.matches("^[a-zA-Z]{2}$")) {
            throw new IllegalArgumentException("Language code must be exactly two letters");
        }
        return langToken.toLowerCase(Locale.ROOT);
    }

    /**
     * Parses maxItems as integer.
     */
    private static int extractAndValidateMaxItems(String maxToken) {
        try {
            return Integer.parseInt(maxToken);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("maxItems must be an integer");
        }
    }

    /**
     * Collects all tokens representing subscription keywords.
     * <p>
     * Everything between the command itself and the language/schedule tokens
     * is considered part of the keyword list.
     */
    private static List<String> extractKeywords(List<String> tokens, int toIndexExclusive) {
        if (toIndexExclusive <= 1) {
            throw new IllegalArgumentException("At least one keyword is required");
        }
        return tokens.subList(1, toIndexExclusive);
    }
}
