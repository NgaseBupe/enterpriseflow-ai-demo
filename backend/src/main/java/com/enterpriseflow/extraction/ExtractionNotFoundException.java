package com.enterpriseflow.extraction;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ExtractionNotFoundException extends ErrorResponseException {

    public ExtractionNotFoundException(UUID documentId) {
        super(HttpStatus.NOT_FOUND, problem(documentId), null);
    }

    private static ProblemDetail problem(UUID documentId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "Document " + documentId + " has not been extracted yet.");
        problem.setTitle("Extraction not found");
        return problem;
    }
}
