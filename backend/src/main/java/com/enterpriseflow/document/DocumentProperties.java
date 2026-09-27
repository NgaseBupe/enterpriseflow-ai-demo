package com.enterpriseflow.document;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * @param storageLocation directory where uploaded files are kept
 * @param maxFileSize     largest accepted upload
 */
@ConfigurationProperties(prefix = "app.documents")
public record DocumentProperties(Path storageLocation, DataSize maxFileSize) {
}
