package com.enterpriseflow.document.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.document.DocumentProperties;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

class LocalDocumentStorageTest {

    @TempDir
    Path root;

    LocalDocumentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalDocumentStorage(new DocumentProperties(root, DataSize.ofMegabytes(10)));
    }

    @Test
    void storesLoadsAndDeletesAFile() {
        storage.store("key-1", TestFiles.PDF);

        assertThat(storage.load("key-1")).isEqualTo(TestFiles.PDF);
        assertThat(root.resolve("key-1")).exists();
        assertThat(root).isDirectoryNotContaining("glob:**.tmp");

        storage.delete("key-1");
        assertThat(root.resolve("key-1")).doesNotExist();
    }

    @Test
    void refusesKeysThatEscapeTheStorageDirectory() {
        assertThatThrownBy(() -> storage.store("../outside", TestFiles.PDF))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load("nested/key"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
