package com.urlshortener.util;

import org.springframework.stereotype.Component;

/**
 * Base62 Encoder — converts a numeric ID to a short alphanumeric code.
 *
 * How it works:
 *   Characters: 0-9 a-z A-Z (62 total)
 *   ID 1000000 → "4c92" (just 4 chars!)
 *
 * Why Base62?
 *   - No collisions: based on unique DB auto-increment ID
 *   - URL-safe: only alphanumeric characters
 *   - Short: 7 chars can represent 62^7 = 3.5 trillion unique URLs
 *
 * Example:
 *   encode(1)       → "1"
 *   encode(62)      → "10"
 *   encode(1000000) → "4c92"
 */
@Component
public class Base62Encoder {

    private static final String CHARACTERS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final int MIN_LENGTH = 6;  // Pad to at least 6 chars

    /**
     * Encode a numeric ID to a Base62 string.
     * @param id auto-increment DB id
     * @return short alphanumeric code (minimum 6 chars)
     */
    public String encode(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be a positive number");
        }

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(CHARACTERS.charAt((int)(id % BASE)));
            id /= BASE;
        }
        sb.reverse();

        // Pad to minimum length with leading '0'
        while (sb.length() < MIN_LENGTH) {
            sb.insert(0, '0');
        }

        return sb.toString();
    }

    /**
     * Decode a Base62 string back to a numeric ID.
     * @param encoded the short code
     * @return the original numeric ID
     */
    public long decode(String encoded) {
        long result = 0;
        for (char c : encoded.toCharArray()) {
            result = result * BASE + CHARACTERS.indexOf(c);
        }
        return result;
    }
}
