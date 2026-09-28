package com.enterpriseflow.ai;

/** A document to extract from. {@code mediaType} is the type detected from the file's content. */
public record DocumentInput(byte[] content, String mediaType, String fileName) {
}
