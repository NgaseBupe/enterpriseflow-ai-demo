package com.enterpriseflow.document.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterpriseflow.TestFiles;
import org.junit.jupiter.api.Test;

class FileSignatureTest {

    @Test
    void recognisesSupportedFormatsByTheirContent() {
        assertThat(FileSignature.detect(TestFiles.PDF)).contains(FileSignature.PDF);
        assertThat(FileSignature.detect(TestFiles.PNG)).contains(FileSignature.PNG);
        assertThat(FileSignature.detect(TestFiles.JPEG)).contains(FileSignature.JPEG);
    }

    @Test
    void rejectsOtherContent() {
        assertThat(FileSignature.detect(TestFiles.TEXT)).isEmpty();
        assertThat(FileSignature.detect(new byte[] {'M', 'Z', (byte) 0x90, 0})).isEmpty();
    }

    @Test
    void rejectsContentShorterThanASignature() {
        assertThat(FileSignature.detect(new byte[] {'%', 'P'})).isEmpty();
        assertThat(FileSignature.detect(new byte[0])).isEmpty();
    }

    @Test
    void mapsToTheCorrectMediaType() {
        assertThat(FileSignature.PDF.mediaType()).isEqualTo("application/pdf");
        assertThat(FileSignature.PNG.mediaType()).isEqualTo("image/png");
        assertThat(FileSignature.JPEG.mediaType()).isEqualTo("image/jpeg");
    }
}
