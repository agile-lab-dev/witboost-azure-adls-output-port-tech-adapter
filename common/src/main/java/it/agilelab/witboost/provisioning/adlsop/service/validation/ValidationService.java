package it.agilelab.witboost.provisioning.adlsop.service.validation;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.model.ProvisionRequest;
import it.agilelab.witboost.provisioning.adlsop.model.Specific;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningRequest;

public interface ValidationService {

    Either<FailedOperation, ProvisionRequest<? extends Specific>> validate(ProvisioningRequest provisioningRequest);
}
