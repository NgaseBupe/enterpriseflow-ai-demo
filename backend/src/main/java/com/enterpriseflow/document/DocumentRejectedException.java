package com.enterpriseflow.document;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** An upload that was refused. Rendered as an RFC 9457 Problem Details response. */
public class DocumentRejectedException extends ErrorResponseException {

    private DocumentRejectedException(HttpStatus status, String type, String title, String detail) {
        super(status, problem(status, type, title, detail), null);
    }

    public static DocumentRejectedException empty() {
        return new DocumentRejectedException(HttpStatus.BAD_REQUEST, "empty-file",
                "Empty file", "The uploaded file is empty.");
    }

    public static DocumentRejectedException tooLarge(long maxBytes) {
        return new DocumentRejectedException(HttpStatus.PAYLOAD_TOO_LARGE, "file-too-large",
                "File too large", "The file exceeds the maximum size of " + (maxBytes / (1024 * 1024)) + " MB.");
    }

    public static DocumentRejectedException unsupportedType() {
        return new DocumentRejectedException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-file-type",
                "Unsupported file type", "Only PDF, PNG and JPEG files are accepted.");
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:enterpriseflow:problem:" + type));
        problem.setTitle(title);
        return problem;
    }
}
