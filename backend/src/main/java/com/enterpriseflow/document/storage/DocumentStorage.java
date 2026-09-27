package com.enterpriseflow.document.storage;

/**
 * Stores and retrieves document files by an opaque key. The local filesystem implementation can be
 * replaced with object storage (S3, Azure Blob) without changing the rest of the module.
 */
public interface DocumentStorage {

    void store(String key, byte[] content);

    byte[] load(String key);

    void delete(String key);
}
