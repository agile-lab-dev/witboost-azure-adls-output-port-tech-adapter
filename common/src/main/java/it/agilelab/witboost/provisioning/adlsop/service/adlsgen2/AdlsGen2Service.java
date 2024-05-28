package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;

public interface AdlsGen2Service {

    Either<FailedOperation, Boolean> containerExists(String storageAccount, String containerName);
}
