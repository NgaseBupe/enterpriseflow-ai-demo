package com.enterpriseflow.document.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileNamesTest {

    @Test
    void keepsAnOrdinaryName() {
        assertThat(FileNames.sanitize("PO 1001.pdf")).isEqualTo("PO 1001.pdf");
    }

    @Test
    void dropsClientPaths() {
        assertThat(FileNames.sanitize("C:\\Users\\me\\Orders\\po.pdf")).isEqualTo("po.pdf");
        assertThat(FileNames.sanitize("../../etc/passwd")).isEqualTo("passwd");
    }

    @Test
    void removesControlCharactersAndWhitespace() {
        assertThat(FileNames.sanitize("  po\r\n.pdf\u0000 ")).isEqualTo("po.pdf");
    }

    @Test
    void removesInvisibleFormattingCharactersUsedToDisguiseExtensions() {
        // U+202E (right-to-left override) makes "po\u202Efdp.exe" display as "poexe.pdf".
        assertThat(FileNames.sanitize("po\u202Efdp.exe")).isEqualTo("pofdp.exe");
        assertThat(FileNames.sanitize("po\u200B.pdf")).isEqualTo("po.pdf");
    }

    @Test
    void fallsBackWhenNothingIsLeft() {
        assertThat(FileNames.sanitize(null)).isEqualTo(FileNames.FALLBACK);
        assertThat(FileNames.sanitize("   ")).isEqualTo(FileNames.FALLBACK);
        assertThat(FileNames.sanitize("folder/")).isEqualTo(FileNames.FALLBACK);
    }

    @Test
    void truncatesLongNames() {
        assertThat(FileNames.sanitize("a".repeat(300) + ".pdf")).hasSize(FileNames.MAX_LENGTH);
    }
}
