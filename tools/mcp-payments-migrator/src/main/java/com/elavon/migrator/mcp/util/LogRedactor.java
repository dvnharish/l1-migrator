package com.elavon.migrator.mcp.util;

import java.util.regex.Pattern;

public final class LogRedactor {
    // Luhn-like sequences, CVV 3-4 digits, track data tokens
    private static final Pattern PAN_PATTERN = Pattern.compile("(?<!\\d)(?:4\\d{12}(?:\\d{3})?|5[1-5]\\d{14}|3[47]\\d{13}|6(?:011|5\\d{2})\\d{12})(?!\\d)");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)(cvv|cvc|cvv2|cvc2)[^0-9]{0,3}(\\d{3,4})");
    private static final Pattern TRACK_PATTERN = Pattern.compile("%B[0-9]{12,19}\\^");

    private LogRedactor() {}

    public static String redactSensitive(String input) {
        if (input == null || input.isEmpty()) return input;
        String out = PAN_PATTERN.matcher(input).replaceAll("[REDACTED_PAN]");
        out = CVV_PATTERN.matcher(out).replaceAll("$1=[REDACTED_CVV]");
        out = TRACK_PATTERN.matcher(out).replaceAll("[REDACTED_TRACK]");
        return out;
    }
}


