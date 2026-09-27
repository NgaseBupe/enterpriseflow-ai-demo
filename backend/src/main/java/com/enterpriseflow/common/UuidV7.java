package com.enterpriseflow.common;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates version 7 UUIDs (RFC 9562): a 48-bit Unix millisecond timestamp followed by random bits.
 *
 * <p>Because the values increase over time, new rows are appended to the end of InnoDB's clustered
 * primary-key index instead of being scattered across it, as random (v4) UUIDs would be.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        return fromTimestamp(System.currentTimeMillis());
    }

    static UUID fromTimestamp(long epochMillis) {
        long randA = RANDOM.nextInt(1 << 12);
        long randB = RANDOM.nextLong();

        long mostSignificant = (epochMillis & 0xFFFF_FFFF_FFFFL) << 16
                | 0x7000L
                | randA;
        long leastSignificant = (randB & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;

        return new UUID(mostSignificant, leastSignificant);
    }
}
