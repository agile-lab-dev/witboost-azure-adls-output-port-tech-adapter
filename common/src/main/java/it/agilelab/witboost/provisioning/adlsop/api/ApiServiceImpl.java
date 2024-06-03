package it.agilelab.witboost.provisioning.adlsop.api;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.common.SpecificProvisionerValidationException;
import it.agilelab.witboost.provisioning.adlsop.model.ProvisionRequest;
import it.agilelab.witboost.provisioning.adlsop.model.Specific;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningRequest;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningStatus;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationError;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationResult;
import it.agilelab.witboost.provisioning.adlsop.service.provision.OutputPortHandler;
import it.agilelab.witboost.provisioning.adlsop.service.validation.ValidationService;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ApiServiceImpl {

    private final ValidationService validationService;
    private final OutputPortHandler outputPortHandler;

    private final String OUTPUTPORT_KIND = "outputport";

    public ApiServiceImpl(ValidationService validationService, OutputPortHandler outputPortHandler) {
        this.validationService = validationService;
        this.outputPortHandler = outputPortHandler;
    }

    public ValidationResult validate(ProvisioningRequest provisioningRequest) {
        Either<FailedOperation, ProvisionRequest<? extends Specific>> validate =
                validationService.validate(provisioningRequest, false);
        return validate.fold(
                failedOperation -> new ValidationResult(false)
                        .error(new ValidationError(failedOperation.problems().stream()
                                .map(Problem::description)
                                .collect(Collectors.toList()))),
                provisionRequest -> new ValidationResult(true));
    }

    public ProvisioningStatus provision(ProvisioningRequest provisioningRequest) {
        Either<FailedOperation, ProvisionRequest<? extends Specific>> eitherValidation =
                validationService.validate(provisioningRequest, true);
        if (eitherValidation.isLeft()) throw new SpecificProvisionerValidationException(eitherValidation.getLeft());
        var provisionRequest = eitherValidation.get();
        switch (provisionRequest.component().getKind()) {
            case OUTPUTPORT_KIND: {
                var eitherDirectoryInfo = outputPortHandler.create(provisionRequest);
                if (eitherDirectoryInfo.isLeft())
                    throw new SpecificProvisionerValidationException(eitherDirectoryInfo.getLeft());
                return new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "")
                        .info(InfoMapper.createDeployInfo(eitherDirectoryInfo.get()));
            }
            default:
                throw new SpecificProvisionerValidationException(
                        unsupportedKind(provisionRequest.component().getKind()));
        }
    }

    public ProvisioningStatus unprovision(ProvisioningRequest provisioningRequest) {
        Either<FailedOperation, ProvisionRequest<? extends Specific>> eitherValidation =
                validationService.validate(provisioningRequest, true);
        if (eitherValidation.isLeft()) throw new SpecificProvisionerValidationException(eitherValidation.getLeft());
        var provisionRequest = eitherValidation.get();
        switch (provisionRequest.component().getKind()) {
            case OUTPUTPORT_KIND: {
                var outcome = outputPortHandler.destroy(provisionRequest);
                if (outcome.isLeft()) throw new SpecificProvisionerValidationException(outcome.getLeft());
                return new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "");
            }
            default:
                throw new SpecificProvisionerValidationException(
                        unsupportedKind(provisionRequest.component().getKind()));
        }
    }

    private FailedOperation unsupportedKind(String kind) {
        return new FailedOperation(Collections.singletonList(new Problem(
                String.format("The kind '%s' of the component is not supported by this Specific Provisioner", kind))));
    }
}
