package com.urlshortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    private Base62Encoder base62Encoder;

    @BeforeEach
    void setUp() {
        base62Encoder = new Base62Encoder();
    }

    @Test
    void testEncodeValidIds() {
        // Test basic numbers
        assertEquals("000001", base62Encoder.encode(1));
        
        // Test base boundary values
        assertEquals("000010", base62Encoder.encode(62));
        
        // Test large values
        assertEquals("004c92", base62Encoder.encode(1000000));
    }

    @Test
    void testEncodeInvalidIdsThrowsException() {
        // Zero ID
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.encode(0));

        // Negative ID
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.encode(-5));
    }

    @Test
    void testDecodeValidCodes() {
        assertEquals(1, base62Encoder.decode("000001"));
        assertEquals(62, base62Encoder.decode("000010"));
        assertEquals(1000000, base62Encoder.decode("004c92"));
    }

    @Test
    void testRoundTrip() {
        long originalId = 123456789L;
        String encoded = base62Encoder.encode(originalId);
        long decoded = base62Encoder.decode(encoded);
        assertEquals(originalId, decoded);
    }
}
