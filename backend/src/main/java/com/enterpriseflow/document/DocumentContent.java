package com.enterpriseflow.document;

/** The stored file of a document, with the content type detected at upload. */
public record DocumentContent(byte[] content, String contentType, String fileName) {
}
