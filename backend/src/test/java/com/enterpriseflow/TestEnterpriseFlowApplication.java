package com.enterpriseflow;

import org.springframework.boot.SpringApplication;

/**
 * Runs the application locally against a throwaway MySQL container: {@code ./mvnw spring-boot:test-run}.
 * No local database installation is needed.
 */
public class TestEnterpriseFlowApplication {

    public static void main(String[] args) {
        SpringApplication.from(EnterpriseFlowApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
