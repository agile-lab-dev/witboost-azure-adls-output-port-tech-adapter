package it.agilelab.witboost.provisioning.adlsop.api;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.common.SpecificProvisionerValidationException;
import it.agilelab.witboost.provisioning.adlsop.model.ProvisionRequest;
import it.agilelab.witboost.provisioning.adlsop.model.ProvisioningResult;
import it.agilelab.witboost.provisioning.adlsop.model.Specific;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.DescriptorKind;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningRequest;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningStatus;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.UpdateAclRequest;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationError;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationResult;
import it.agilelab.witboost.provisioning.adlsop.parser.Parser;
import it.agilelab.witboost.provisioning.adlsop.service.provision.OutputPortHandler;
import it.agilelab.witboost.provisioning.adlsop.service.validation.ValidationService;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApiServiceImpl {

    private final ValidationService validationService;
    private final OutputPortHandler outputPortHandler;

    private final String OUTPUTPORT_KIND = "outputport";

    public ApiServiceImpl(ValidationService validationService, OutputPortHandler outputPortHandler) {
        this.validationService = validationService;
        this.outputPortHandler = outputPortHandler;
    }

    public ValidationResult validate(ProvisioningRequest provisioningRequest) {
        try {
            Either<FailedOperation, ProvisionRequest<? extends Specific>> validate =
                    validationService.validate(provisioningRequest, false);
            return validate.fold(
                    failedOperation -> new ValidationResult(false)
                            .error(new ValidationError(failedOperation.problems().stream()
                                    .map(Problem::description)
                                    .collect(Collectors.toList()))),
                    provisionRequest -> new ValidationResult(true));
        } catch (ConstraintViolationException validationException) {
            return new ValidationResult(false)
                    .error(new ValidationError(validationException.getConstraintViolations().stream()
                            .map(Problem::fromConstraintViolation)
                            .map(Problem::description)
                            .collect(Collectors.toList())));
        }
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

    public ProvisioningStatus updateAcl(UpdateAclRequest updateAclRequest) {
        log.info("Starting updating Access Control Lists");
        // Converting the ProvisionInfo.request to a ProvisioningRequest, to exploit validation methods already in place
        ProvisioningRequest provisioningRequest = new ProvisioningRequest(
                DescriptorKind.COMPONENT_DESCRIPTOR,
                updateAclRequest.getProvisionInfo().getRequest(),
                Boolean.FALSE);

        Either<FailedOperation, ProvisionRequest<? extends Specific>> eitherValidation =
                validationService.validate(provisioningRequest, false);
        if (eitherValidation.isLeft()) throw new SpecificProvisionerValidationException(eitherValidation.getLeft());

        ProvisionRequest<? extends Specific> provisionRequest = eitherValidation.get();

        switch (provisionRequest.component().getKind()) {
            case OUTPUTPORT_KIND -> {
                var eitherStorageInfo =
                        Parser.parseObject(updateAclRequest.getProvisionInfo().getResult(), ProvisioningResult.class);
                if (eitherStorageInfo.isRight()) {
                    return outputPortHandler
                            .updateAcl(updateAclRequest.getRefs(), provisionRequest, eitherStorageInfo.get())
                            .getOrElseThrow(failedOperation -> {
                                throw new SpecificProvisionerValidationException(failedOperation);
                            });
                } else throw new SpecificProvisionerValidationException(eitherStorageInfo.getLeft());
            }
            default -> {
                log.error(String.format(
                        "Component kind '%s' is invalid",
                        provisionRequest.component().getKind()));
                throw new SpecificProvisionerValidationException(
                        unsupportedKind(provisionRequest.component().getKind()));
            }
        }
    }

    private FailedOperation unsupportedKind(String kind) {
        return new FailedOperation(Collections.singletonList(new Problem(
                String.format("The kind '%s' of the component is not supported by this Specific Provisioner", kind))));
    }
}
