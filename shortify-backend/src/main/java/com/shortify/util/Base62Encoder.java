package com.shortify.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Encodes a numeric ID to a Base62 string.
     * Base62 uses 62 characters (0-9, a-z, A-Z) to represent numbers.
     * This ensures no collisions since each ID produces a unique short code.
     *
     * Examples:
     * 1 → "1"
     * 62 → "10"
     * 3843 → "zzz"
     * 238328 → "1000"
     *
     * @param id the numeric ID to encode
     * @return Base62 encoded string
     */
    public String encode(long id) {
        if (id == 0) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(BASE62.charAt((int) (id % 62)));
            id /= 62;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to its numeric ID.
     * This is useful for verification or debugging.
     *
     * @param encoded the Base62 encoded string
     * @return the decoded numeric ID
     */
    public long decode(String encoded) {
        long id = 0;
        for (char c : encoded.toCharArray()) {
            id = id * 62 + BASE62.indexOf(c);
        }
        return id;
    }
}
