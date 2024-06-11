package it.agilelab.witboost.provisioning.adlsop.controller;

import static it.agilelab.witboost.provisioning.adlsop.common.TestFixtures.buildConstraintViolation;

import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.common.SpecificProvisionerValidationException;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.RequestValidationError;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.SystemError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(SpecificProvisionerExceptionHandler.class)
public class SpecificProvisionerExceptionHandlerTest {

    @InjectMocks
    SpecificProvisionerExceptionHandler specificProvisionerExceptionHandler;

    @Test
    void testHandleConflictSystemError() {
        RuntimeException runtimeException = new RuntimeException();
        String expectedError =
                "An unexpected error occurred while processing the request. Please try again later. If the issue still persists, contact the platform team for assistance! Details: ";

        SystemError error = specificProvisionerExceptionHandler.handleSystemError(runtimeException);

        Assertions.assertTrue(error.getError().startsWith(expectedError));
    }

    @Test
    void testHandleConflictRequestValidationError() {
        String expectedError = "Validation error";
        SpecificProvisionerValidationException specificProvisionerValidationException =
                new SpecificProvisionerValidationException(
                        new FailedOperation(Collections.singletonList(new Problem(expectedError))));

        RequestValidationError requestValidationError =
                specificProvisionerExceptionHandler.handleValidationException(specificProvisionerValidationException);

        Assertions.assertEquals(1, requestValidationError.getErrors().size());
        requestValidationError.getErrors().forEach(e -> Assertions.assertEquals(expectedError, e));
    }

    @Test
    void testHandleConstraintValidationError() {
        Set<ConstraintViolation<?>> violations = Set.of(
                buildConstraintViolation("is not valid", "path.to.field"),
                buildConstraintViolation("must not be null", "other.field"));
        ConstraintViolationException error = new ConstraintViolationException(violations);

        RequestValidationError requestValidationError =
                specificProvisionerExceptionHandler.handleValidationException(error);

        var expectedMessage =
                "Validation on the received descriptor failed, check the error details for more information";
        var expectedErrors = Set.of("path.to.field is not valid", "other.field must not be null");

        Assertions.assertEquals(2, requestValidationError.getErrors().size());
        Assertions.assertEquals(expectedErrors, Set.copyOf(requestValidationError.getErrors()));
        Assertions.assertEquals(
                requestValidationError.getErrors(),
                requestValidationError.getMoreInfo().getProblems());
        Assertions.assertEquals(expectedMessage, requestValidationError.getUserMessage());
    }

    @Test
    void testHandleConstraintValidationErrorSingle() {
        Set<ConstraintViolation<?>> violations = Set.of(buildConstraintViolation("is not valid", "path.to.field"));
        ConstraintViolationException error = new ConstraintViolationException(violations);

        RequestValidationError requestValidationError =
                specificProvisionerExceptionHandler.handleValidationException(error);

        var expectedMessage =
                "Validation on the received descriptor failed, check the error details for more information";
        var expectedErrors = Set.of("path.to.field is not valid");

        Assertions.assertEquals(1, requestValidationError.getErrors().size());
        Assertions.assertEquals(expectedErrors, Set.copyOf(requestValidationError.getErrors()));
        Assertions.assertEquals(
                requestValidationError.getErrors(),
                requestValidationError.getMoreInfo().getProblems());
        Assertions.assertEquals(expectedMessage, requestValidationError.getUserMessage());
        Assertions.assertEquals("path.to.field", requestValidationError.getInputErrorField());
    }
}
