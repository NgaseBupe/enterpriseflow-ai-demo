package com.enterpriseflow.document.internal;

/** Cleans the client-supplied file name, which is stored as metadata only and never used as a path. */
public final class FileNames {

    static final int MAX_LENGTH = 255;
    static final String FALLBACK = "unnamed";

    private FileNames() {
    }

    public static String sanitize(String original) {
        if (original == null) {
            return FALLBACK;
        }
        // Some browsers send a full client path such as C:\Users\me\po.pdf; keep only the last segment.
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
        // Remove control characters and invisible formatting characters (Unicode category Cf), such as the
        // right-to-left override that makes "po\u202Efdp.exe" display as "poexe.pdf".
        name = name.replaceAll("[\\p{Cntrl}\\p{Cf}]", "").strip();
        if (name.isEmpty()) {
            return FALLBACK;
        }
        return name.length() > MAX_LENGTH ? name.substring(0, MAX_LENGTH) : name;
    }
}
