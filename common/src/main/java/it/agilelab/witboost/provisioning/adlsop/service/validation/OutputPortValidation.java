package it.agilelab.witboost.provisioning.adlsop.service.validation;

import static io.vavr.control.Either.left;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.Component;
import it.agilelab.witboost.provisioning.adlsop.model.DataProduct;
import it.agilelab.witboost.provisioning.adlsop.model.Specific;
import java.util.Collections;

public class OutputPortValidation {

    public static Either<FailedOperation, Void> validate(
            DataProduct dataProduct, Component<? extends Specific> component) {
        // TODO Remember to implement the validation for the output port.
        return left(new FailedOperation(Collections.singletonList(
                new Problem("Implement the validation for output port based on your requirements!"))));
    }
}
