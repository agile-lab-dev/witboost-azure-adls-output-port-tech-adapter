package it.agilelab.witboost.provisioning.adlsop.service.validation;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2Service;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

@org.springframework.stereotype.Component
@Validated
public class OutputPortValidator {
    private static final Logger logger = LoggerFactory.getLogger(OutputPortValidator.class);

    private final AdlsGen2Service adlsGen2Service;

    public OutputPortValidator(AdlsGen2Service adlsGen2Service) {
        this.adlsGen2Service = adlsGen2Service;
    }

    /**
     * Validates a component as an output port and that it complies with the Output Port specific schema
     * @param dataProduct Data product context
     * @param component Component to be validated
     * @param validateStorageAccountExists If true, the method will query the ADLS Gen-2 instance and validate
     *                                     the storage account and container existence
     */
    public Either<FailedOperation, Void> validate(
            DataProduct dataProduct,
            @Valid Component<? extends Specific> component,
            @NotNull boolean validateStorageAccountExists) {
        logger.info("Checking component with ID {} is of type OutputPort", component.getId());
        if (component instanceof OutputPort<? extends Specific> op) {
            logger.info("The received component is an Output Port");
            if (op.getSpecific() instanceof OutputPortSpecific specific) {
                if (validateStorageAccountExists) {
                    return this.adlsGen2Service
                            .containerExists(specific.getStorageAccount(), specific.getContainer())
                            .flatMap(exists -> {
                                if (!exists) {
                                    String errorMessage = String.format(
                                            "The container '%s' on storage account '%s' doesn't exist",
                                            specific.getContainer(), specific.getStorageAccount());
                                    return left(
                                            new FailedOperation(Collections.singletonList(new Problem(errorMessage))));
                                }
                                return right(null);
                            });
                }
            } else {
                String errorMessage = String.format(
                        "The specific section of the component %s is not of type OutputPortSpecific",
                        component.getId());
                logger.error(errorMessage);
                return left(new FailedOperation(Collections.singletonList(new Problem(errorMessage))));
            }
        } else {
            String errorMessage = String.format("The component %s is not of type OutputPort", component.getId());
            logger.error(errorMessage);
            return left(new FailedOperation(Collections.singletonList(new Problem(errorMessage))));
        }
        logger.info("Validation of OutputPort {} completed successfully", component.getId());
        return right(null);
    }
}
