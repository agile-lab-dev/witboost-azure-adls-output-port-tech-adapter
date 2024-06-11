package it.agilelab.witboost.provisioning.adlsop.common;

import static it.agilelab.witboost.provisioning.adlsop.common.TestFixtures.buildConstraintViolation;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProblemTest {

    @Test
    void fromConstraintViolation() {

        var expectedDesc = "path.to.field is not valid";
        var problem = Problem.fromConstraintViolation(buildConstraintViolation("is not valid", "path.to.field"));

        assertEquals(expectedDesc, problem.description());
    }
}
