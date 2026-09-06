package se.pbt.mn.telegram.format;

import java.util.regex.Pattern;

/**
 * Formats outgoing text messages for Telegram.
 * <p>
 * Handles MarkdownV2 escaping and JSON-safe quoting
 * so messages can be sent without syntax or parsing errors.
 */
public final class TelegramOutputFormatter {

    /**
     * Matches all characters that must be escaped for Telegram MarkdownV2.
     * See: https://core.telegram.org/bots/api#markdownv2-style
     */
    private static final Pattern MDV2_SPECIALS =
            // There is no redundant character escape! Changing the regex breaks Telegram communication!
            Pattern.compile("([_\\*\\[\\]\\(\\)~`>#+\\-=|\\{\\}\\.\\!])");

    private TelegramOutputFormatter() {}

    /**
     * Escapes all special MarkdownV2 characters in the given text.
     * <p>
     * This ensures that Telegram displays the text literally, without misinterpreting
     * it as Markdown formatting.
     */
    public static String escapeMarkdown(String text) {
        if (text == null || text.isEmpty()) return "";
        return MDV2_SPECIALS.matcher(text).replaceAll("\\\\$1");
    }

    /**
     * Escapes the characters MarkdownV2 requires inside an inline link's URL part --
     * {@code [text](url)} -- which is a narrower rule than {@link #escapeMarkdown}: only
     * ')' and '\' need escaping there, not the full special-character set.
     */
    public static String escapeLinkUrl(String url) {
        if (url == null) return "";
        return url.replace("\\", "\\\\").replace(")", "\\)");
    }

    /**
     * Converts the given text into a JSON-safe string literal.
     * <p>
     * Escapes backslashes, double quotes, and control characters (newlines included) so
     * multi-line text still produces valid JSON.
     */
    public static String json(String text) {
        if (text == null) return "\"\"";

        StringBuilder sb = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
