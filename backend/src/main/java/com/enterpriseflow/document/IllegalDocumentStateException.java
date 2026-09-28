package com.enterpriseflow.document;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** An action that the document's current status does not allow. Rendered as 409 Conflict. */
public class IllegalDocumentStateException extends ErrorResponseException {

    public IllegalDocumentStateException(DocumentStatus current, String action) {
        super(HttpStatus.CONFLICT, problem(current, action), null);
    }

    private static ProblemDetail problem(DocumentStatus current, String action) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Cannot " + action + " while the document's status is \"" + current.label() + "\".");
        problem.setType(URI.create("urn:enterpriseflow:problem:invalid-document-state"));
        problem.setTitle("Action not allowed in the current state");
        problem.setProperty("currentStatus", current.name());
        return problem;
    }
}
