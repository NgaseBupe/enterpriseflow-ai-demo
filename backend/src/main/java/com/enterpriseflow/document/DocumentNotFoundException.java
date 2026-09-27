package com.enterpriseflow.document;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class DocumentNotFoundException extends ErrorResponseException {

    public DocumentNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, problem(id), null);
    }

    private static ProblemDetail problem(UUID id) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No document with ID " + id + ".");
        problem.setTitle("Document not found");
        return problem;
    }
}
