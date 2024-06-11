package it.agilelab.witboost.provisioning.adlsop.common;

import jakarta.validation.ConstraintViolation;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;

public class TestFixtures {
    public static ConstraintViolation<?> buildConstraintViolation(String interpolatedMessage, String path) {
        return ConstraintViolationImpl.forBeanValidation(
                "",
                null,
                null,
                interpolatedMessage,
                null,
                null,
                null,
                null,
                PathImpl.createPathFromString(path),
                null,
                null);
    }
}
