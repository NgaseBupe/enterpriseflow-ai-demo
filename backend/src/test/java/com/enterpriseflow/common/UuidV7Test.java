package com.enterpriseflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void hasVersion7AndRfcVariant() {
        UUID uuid = UuidV7.generate();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void encodesTheTimestampInTheFirst48Bits() {
        long millis = 1_790_000_000_000L;

        UUID uuid = UuidV7.fromTimestamp(millis);

        assertThat(uuid.getMostSignificantBits() >>> 16).isEqualTo(millis);
    }

    @Test
    void laterTimestampsSortAfterEarlierOnes() {
        UUID earlier = UuidV7.fromTimestamp(1_790_000_000_000L);
        UUID later = UuidV7.fromTimestamp(1_790_000_000_001L);

        assertThat(Long.compareUnsigned(later.getMostSignificantBits(), earlier.getMostSignificantBits()))
                .isPositive();
    }
}
