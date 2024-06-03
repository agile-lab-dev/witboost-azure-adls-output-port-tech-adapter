package it.agilelab.witboost.provisioning.adlsop.service.provision;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2Service;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OutputPortHandler {
    private final Logger logger = LoggerFactory.getLogger(OutputPortHandler.class);

    private final AdlsGen2Service adlsGen2Service;

    public OutputPortHandler(AdlsGen2Service adlsGen2Service) {
        this.adlsGen2Service = adlsGen2Service;
    }

    public <T extends Specific> Either<FailedOperation, AdlsGen2DirectoryInfo> create(
            ProvisionRequest<T> provisionRequest) {
        if (provisionRequest.component() instanceof OutputPort<T> op) {
            var eitherSpecific = getOutputPortSpecific(provisionRequest);
            if (eitherSpecific.isLeft()) return left(eitherSpecific.getLeft());
            var specific = eitherSpecific.get();
            return adlsGen2Service
                    .createDirectory(specific.getStorageAccount(), specific.getContainer(), specific.getPath())
                    .map(info -> {
                        info.setFileFormat(specific.getFileFormat());
                        return info;
                    });
        } else {
            return left(wrongComponentType());
        }
    }

    public <T extends Specific> Either<FailedOperation, Void> destroy(ProvisionRequest<T> provisionRequest) {
        if (provisionRequest.component() instanceof OutputPort<T> op) {
            var eitherSpecific = getOutputPortSpecific(provisionRequest);
            if (eitherSpecific.isLeft()) return left(eitherSpecific.getLeft());
            var specific = eitherSpecific.get();

            return adlsGen2Service.deleteDirectory(
                    specific.getStorageAccount(),
                    specific.getContainer(),
                    specific.getPath(),
                    provisionRequest.removeData());
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
            logger.error(errorMessage);
            return left(new FailedOperation(Collections.singletonList(new Problem(errorMessage))));
        }
    }

    private FailedOperation wrongComponentType() {
        String errorMessage = "The component type is not of expected type OutputPort";
        logger.error(errorMessage);
        return new FailedOperation(Collections.singletonList(new Problem(errorMessage)));
    }
}
