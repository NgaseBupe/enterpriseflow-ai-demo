package com.enterpriseflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails the build if one module reaches into another module's internal packages. */
class ModularityTests {

    @Test
    void moduleBoundariesAreRespected() {
        ApplicationModules.of(EnterpriseFlowApplication.class).verify();
    }
}
