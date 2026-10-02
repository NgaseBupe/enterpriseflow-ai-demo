package com.enterpriseflow.extraction;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** A correction that cannot be applied to the extraction as it currently stands. */
public class ExtractionConflictException extends ErrorResponseException {

    private ExtractionConflictException(HttpStatus status, String type, String title, String detail) {
        super(status, problem(status, type, title, detail), null);
    }

    /** The reviewer edited an older version: someone else saved changes in the meantime. */
    public static ExtractionConflictException staleVersion() {
        return new ExtractionConflictException(HttpStatus.CONFLICT, "stale-version", "Changed by someone else",
                "This extraction was changed by someone else after you opened it. Reload to see the latest version.");
    }

    /** Lines can be corrected but not added or removed yet; that arrives with the line-items grid. */
    public static ExtractionConflictException linesDoNotMatch() {
        return new ExtractionConflictException(HttpStatus.BAD_REQUEST, "lines-do-not-match", "Lines do not match",
                "Send exactly the existing lines, identified by their line numbers.");
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:enterpriseflow:problem:" + type));
        problem.setTitle(title);
        return problem;
    }
}
