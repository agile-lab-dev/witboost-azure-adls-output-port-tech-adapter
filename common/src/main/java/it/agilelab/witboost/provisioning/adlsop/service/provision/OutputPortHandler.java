package it.agilelab.witboost.provisioning.adlsop.service.provision;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.model.azure.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningStatus;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureMapper;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2Service;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OutputPortHandler {

    private final AdlsGen2Service adlsGen2Service;
    private final AzureMapper azureMapper;

    public OutputPortHandler(AdlsGen2Service adlsGen2Service, AzureMapper azureMapper) {
        this.adlsGen2Service = adlsGen2Service;
        this.azureMapper = azureMapper;
    }

    public <T extends Specific> Either<FailedOperation, AdlsGen2DirectoryInfo> create(
            ProvisionRequest<T> provisionRequest) {
        if (provisionRequest.component() instanceof OutputPort<T> op) {
            var eitherSpecific = getOutputPortSpecific(provisionRequest);
            if (eitherSpecific.isLeft()) return left(eitherSpecific.getLeft());
            var specific = eitherSpecific.get();

            if (op.getDependsOn() != null && !op.getDependsOn().isEmpty()) {
                String storageComponentId = op.getDependsOn().get(0);
                return provisionRequest
                        .dataProduct()
                        .getDeployInfo(storageComponentId, StorageDeployInfo.class)
                        .flatMap(StorageDeployInfo::getStorageAccountName)
                        .flatMap(storageAccountName -> adlsGen2Service
                                .createDirectory(storageAccountName, specific.getContainer(), specific.getPath())
                                .map(info -> {
                                    info.setFileFormat(specific.getFileFormat());
                                    return info;
                                }));
            } else {
                return left(missingDependentStorageArea());
            }

        } else {
            return left(wrongComponentType());
        }
    }

    public <T extends Specific> Either<FailedOperation, Void> destroy(ProvisionRequest<T> provisionRequest) {
        if (provisionRequest.component() instanceof OutputPort<T> op) {
            var eitherSpecific = getOutputPortSpecific(provisionRequest);
            if (eitherSpecific.isLeft()) return left(eitherSpecific.getLeft());
            var specific = eitherSpecific.get();

            if (op.getDependsOn() != null && !op.getDependsOn().isEmpty()) {
                String storageComponentId = op.getDependsOn().get(0);
                return provisionRequest
                        .dataProduct()
                        .getDeployInfo(storageComponentId, StorageDeployInfo.class)
                        .flatMap(StorageDeployInfo::getStorageAccountName)
                        .flatMap(storageAccountName -> adlsGen2Service.deleteDirectory(
                                storageAccountName,
                                specific.getContainer(),
                                specific.getPath(),
                                provisionRequest.removeData()));
            } else {
                return left(missingDependentStorageArea());
            }

        } else {
            return left(wrongComponentType());
        }
    }

    public <T extends Specific> Either<FailedOperation, ProvisioningStatus> updateAcl(
            Collection<String> refs, ProvisionRequest<T> provisionRequest) {
        if (provisionRequest.component() instanceof OutputPort<T> op) {
            var eitherSpecific = getOutputPortSpecific(provisionRequest);
            if (eitherSpecific.isLeft()) return left(eitherSpecific.getLeft());
            var specific = eitherSpecific.get();

            if (op.getDependsOn() != null && !op.getDependsOn().isEmpty()) {
                String storageComponentId = op.getDependsOn().get(0);
                return provisionRequest
                        .dataProduct()
                        .getDeployInfo(storageComponentId, StorageDeployInfo.class)
                        .flatMap(StorageDeployInfo::getStorageAccountName)
                        .flatMap(storageAccountName -> {
                            Map<String, Either<Throwable, String>> res = azureMapper.map(Set.copyOf(refs));
                            var eitherObjectsIds = res.values().stream()
                                    .map(eitherObjectId -> eitherObjectId.mapLeft(throwable -> new FailedOperation(
                                            Collections.singletonList(new Problem(throwable.getMessage(), throwable)))))
                                    .toList();

                            var allIds = eitherObjectsIds.stream()
                                    .filter(Either::isRight)
                                    .map(Either::get)
                                    .toList();

                            var updateAclResult = adlsGen2Service.updateAcl(
                                    storageAccountName, specific.getContainer(), specific.getPath(), allIds);

                            ArrayList<Problem> problems = eitherObjectsIds.stream()
                                    .filter(Either::isLeft)
                                    .map(Either::getLeft)
                                    .flatMap(x -> x.problems().stream())
                                    .collect(Collectors.toCollection(ArrayList::new));

                            if (updateAclResult.isLeft()) {
                                problems.addAll(updateAclResult.getLeft().problems());
                            }

                            if (problems.isEmpty()) {
                                log.info(
                                        "Access Control Lists updated successfully: no problems encountered while updating Access Control Lists");
                                return right(new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, ""));
                            } else {
                                log.warn(
                                        "Access Control Lists updated: some issues were encountered while updating Access Control Lists");
                                problems.forEach(problem -> log.warn(problem.description()));
                                return left(new FailedOperation(problems));
                            }
                        });
            } else {
                return left(missingDependentStorageArea());
            }

        } else {
            return left(wrongComponentType());
        }
    }

    private <T extends Specific> Either<FailedOperation, OutputPortSpecific> getOutputPortSpecific(
            ProvisionRequest<T> provisionRequest) {
        if (provisionRequest.component().getSpecific() instanceof OutputPortSpecific ss) {
            return right(ss);
        } else {
            String errorMessage = String.format(
                    "The specific section of the component %s is not of type OutputPortSpecific",
                    provisionRequest.component().getId());
            log.error(errorMessage);
            return left(new FailedOperation(Collections.singletonList(new Problem(errorMessage))));
        }
    }

    private FailedOperation missingDependentStorageArea() {
        String errorMessage = "The output port has not a corresponding dependent storage area";
        log.error(errorMessage);
        return new FailedOperation(Collections.singletonList(new Problem(errorMessage)));
    }

    private FailedOperation wrongComponentType() {
        String errorMessage = "The component type is not of expected type OutputPort";
        log.error(errorMessage);
        return new FailedOperation(Collections.singletonList(new Problem(errorMessage)));
    }
}
