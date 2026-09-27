package com.enterpriseflow;

import java.nio.charset.StandardCharsets;

/** Minimal synthetic files that start with the real signatures of each format. */
public final class TestFiles {

    public static final byte[] PDF = "%PDF-1.7\n1 0 obj << /Type /Catalog >> endobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    public static final byte[] TEXT = "Purchase order 1001, please deliver...".getBytes(StandardCharsets.UTF_8);

    private TestFiles() {
    }
}
