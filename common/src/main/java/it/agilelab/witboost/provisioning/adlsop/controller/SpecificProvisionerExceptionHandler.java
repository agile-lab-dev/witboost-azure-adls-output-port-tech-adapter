package it.agilelab.witboost.provisioning.adlsop.controller;

import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.common.SpecificProvisionerValidationException;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ErrorMoreInfo;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.RequestValidationError;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.SystemError;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for the API layer.
 *
 * <p>The following methods wrap generic exceptions into 400 and 500 errors. Implement your own
 * exception handlers based on the business exception that the provisioner throws. No further
 * modifications need to be done outside this file to make it work, as Spring identifies at startup
 * the handlers with the @ExceptionHandler annotation
 */
@RestControllerAdvice
public class SpecificProvisionerExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(SpecificProvisionerExceptionHandler.class);

    @ExceptionHandler({SpecificProvisionerValidationException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected RequestValidationError handleValidationException(Exception ex) {
        List<String> problems = new ArrayList<>();
        var error = new RequestValidationError()
                .userMessage(
                        "Validation on the received descriptor failed, check the error details for more information");
        if (ex instanceof SpecificProvisionerValidationException customException) {
            problems = customException.getFailedOperation().problems().stream()
                    .map(Problem::description)
                    .collect(Collectors.toList());

        } else if (ex instanceof ConstraintViolationException validationException) {
            problems = validationException.getConstraintViolations().stream()
                    .map(Problem::fromConstraintViolation)
                    .map(Problem::description)
                    .collect(Collectors.toList());
            error = error.inputErrorField(
                    validationException.getConstraintViolations().size() == 1
                            ? validationException.getConstraintViolations().stream()
                                    .map(cv -> cv.getPropertyPath().toString())
                                    .findFirst()
                                    .get()
                            : null);
        }
        return error.errors(problems).moreInfo(new ErrorMoreInfo(problems, Collections.EMPTY_LIST));
    }

    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected SystemError handleSystemError(RuntimeException ex) {
        String errorMessage = String.format(
                "An unexpected error occurred while processing the request. Please try again later. If the issue still persists, contact the platform team for assistance! Details: %s",
                ex.getMessage());
        logger.error(errorMessage, ex);
        return new SystemError(errorMessage);
    }
}
