package it.agilelab.witboost.provisioning.adlsop.service.validation;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.model.ProvisionRequest;
import it.agilelab.witboost.provisioning.adlsop.model.Specific;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningRequest;

public interface ValidationService {

    /**
     * Validates a provisioning request, performing schema validations and environment checks
     * @param provisioningRequest Provisioning request to be validated
     * @param validateStorageAccountExists If true, the method will query the ADLS Gen-2 instance and validate
     *                                     the storage account and container existence
     * @return
     */
    Either<FailedOperation, ProvisionRequest<? extends Specific>> validate(
            ProvisioningRequest provisioningRequest, boolean validateStorageAccountExists);
}
