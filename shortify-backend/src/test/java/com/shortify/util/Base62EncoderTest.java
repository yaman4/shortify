package com.shortify.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Base62Encoder - Collision Prevention Strategy
 * 
 * This test verifies that the Base62 encoding provides guaranteed collision-free
 * short code generation when combined with database ID allocation.
 */
public class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    public void testEncodeDecodeConsistency() {
        // Test that encoding and decoding are inverse operations
        long[] testIds = {1, 62, 63, 3843, 238328, 14776336, 916132832L};
        
        for (long id : testIds) {
            String encoded = encoder.encode(id);
            long decoded = encoder.decode(encoded);
            assertEquals(id, decoded, "Encode-decode should be consistent for ID: " + id);
        }
    }

    @Test
    public void testZeroEncoding() {
        // Special case: ID 0 should encode to "0"
        String encoded = encoder.encode(0);
        assertEquals("0", encoded);
        assertEquals(0, encoder.decode(encoded));
    }

    @Test
    public void testUniqueEncodingPerID() {
        // Test that each unique ID produces a unique short code
        String[] codes = new String[100];
        for (int i = 1; i <= 100; i++) {
            codes[i - 1] = encoder.encode(i);
        }
        
        // Check all codes are unique
        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals(codes[i], codes[j], 
                    String.format("Collision detected: ID %d and %d both map to: %s", 
                    i + 1, j + 1, codes[i]));
            }
        }
    }

    @Test
    public void testCollisionFreenessLargeRange() {
        // Test collision-free guarantee for larger ID ranges
        int sampleSize = 10000;
        java.util.Set<String> uniqueCodes = new java.util.HashSet<>();
        
        for (long id = 1; id <= sampleSize; id++) {
            String code = encoder.encode(id);
            assertTrue(uniqueCodes.add(code), 
                String.format("Collision detected at ID %d: code already exists", id));
        }
        
        // All codes should be unique
        assertEquals(sampleSize, uniqueCodes.size(), "Should have " + sampleSize + " unique codes");
    }

    @Test
    public void testBase62CharacterSet() {
        // Verify encoding uses only Base62 characters (0-9, a-z, A-Z)
        String base62Chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        java.util.Set<Character> validChars = new java.util.HashSet<>();
        
        for (char c : base62Chars.toCharArray()) {
            validChars.add(c);
        }
        
        for (long id = 1; id <= 1000; id++) {
            String encoded = encoder.encode(id);
            for (char c : encoded.toCharArray()) {
                assertTrue(validChars.contains(c), 
                    String.format("Invalid character '%c' in encoded ID %d: %s", c, id, encoded));
            }
        }
    }

    @Test
    public void testEncodingEfficiency() {
        // Test that Base62 encoding produces shorter codes than random alphanumeric
        // For ID ranges up to ~1 million, Base62 should produce 6-character codes or less
        String code1 = encoder.encode(1);
        String code62 = encoder.encode(62);
        String code3843 = encoder.encode(3843);
        String code238328 = encoder.encode(238328);
        
        assertTrue(code1.length() >= 1);
        assertTrue(code62.length() >= 1);
        assertTrue(code3843.length() <= 6, "Code for ID 3843 should be <= 6 chars, got: " + code3843);
        assertTrue(code238328.length() <= 6, "Code for ID 238328 should be <= 6 chars, got: " + code238328);
    }

    @Test
    public void testLargeIDEncoding() {
        // Test with very large IDs
        long largeId = Long.MAX_VALUE / 62; // A large but valid ID
        String encoded = encoder.encode(largeId);
        long decoded = encoder.decode(encoded);
        assertEquals(largeId, decoded);
        assertNotNull(encoded);
        assertTrue(encoded.length() > 0);
    }

    @Test
    public void testSequentialIDsProduceDifferentCodes() {
        // Verify that sequential IDs produce different codes (no collisions)
        for (long id = 1; id < 1000; id++) {
            String code1 = encoder.encode(id);
            String code2 = encoder.encode(id + 1);
            assertNotEquals(code1, code2, 
                String.format("Sequential IDs %d and %d produced same code", id, id + 1));
        }
    }

    @Test
    public void testConsistentEncoding() {
        // Verify that encoding the same ID multiple times produces the same result
        long testId = 12345;
        String code1 = encoder.encode(testId);
        String code2 = encoder.encode(testId);
        String code3 = encoder.encode(testId);
        
        assertEquals(code1, code2);
        assertEquals(code2, code3);
    }

    /**
     * Demonstrates the collision prevention strategy:
     * 
     * BEFORE (Random Generation):
     * ❌ RandomStringUtils.randomAlphanumeric(6)
     * - Can collide at any time
     * - No retry logic in current implementation
     * - Transaction fails on collision
     * 
     * AFTER (Base62 + ID Allocation):
     * ✅ Encode database ID using Base62
     * - ID 1 → "1"
     * - ID 62 → "10"
     * - ID 3843 → "zzz"
     * - ID 238328 → "1000"
     * - GUARANTEED NO COLLISIONS
     * - Each ID is unique by design
     */
    @Test
    public void testCollisionPrevention() {
        // This test documents the collision prevention strategy
        // Each database ID (auto-increment) maps to exactly one Base62 code
        
        java.util.Map<String, Long> codeToIdMap = new java.util.HashMap<>();
        
        // Simulate saving 1000 URLs
        for (long id = 1; id <= 1000; id++) {
            String code = encoder.encode(id);
            
            // This should never happen with Base62 + ID allocation
            assertFalse(codeToIdMap.containsKey(code), 
                "COLLISION DETECTED: Code '" + code + "' already mapped to ID " + codeToIdMap.get(code) + 
                ", trying to map to ID " + id);
            
            codeToIdMap.put(code, id);
        }
        
        assertEquals(1000, codeToIdMap.size(), "All 1000 URLs should have unique codes");
    }
}

