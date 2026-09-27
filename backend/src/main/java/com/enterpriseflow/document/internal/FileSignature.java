package com.enterpriseflow.document.internal;

import java.util.Arrays;
import java.util.Optional;

/**
 * Identifies supported file types from their first bytes ("magic numbers") rather than trusting the
 * file name or the Content-Type header sent by the client, both of which are trivial to fake.
 */
public enum FileSignature {

    PDF("application/pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
    PNG("image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
    JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    private final String mediaType;
    private final byte[] magic;

    FileSignature(String mediaType, byte[] magic) {
        this.mediaType = mediaType;
        this.magic = magic;
    }

    public String mediaType() {
        return mediaType;
    }

    public static Optional<FileSignature> detect(byte[] content) {
        return Arrays.stream(values())
                .filter(signature -> signature.matches(content))
                .findFirst();
    }

    private boolean matches(byte[] content) {
        return content.length >= magic.length
                && Arrays.equals(content, 0, magic.length, magic, 0, magic.length);
    }
}
