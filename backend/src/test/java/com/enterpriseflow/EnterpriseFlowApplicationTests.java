package com.enterpriseflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EnterpriseFlowApplicationTests {

    @Test
    void contextLoadsAndSchemaMatchesEntities() {
        // Startup runs the Flyway migrations and Hibernate schema validation (ddl-auto=validate).
    }
}
