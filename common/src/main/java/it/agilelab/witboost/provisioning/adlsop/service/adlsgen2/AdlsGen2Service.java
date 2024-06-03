package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.model.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.model.StorageAccountInfo;

public interface AdlsGen2Service {

    Either<FailedOperation, Boolean> containerExists(String storageAccount, String containerName);

    Either<FailedOperation, StorageAccountInfo> getStorageAccountInfo(String storageAccount);

    Either<FailedOperation, AdlsGen2DirectoryInfo> createDirectory(
            String storageAccount, String containerName, String path);

    Either<FailedOperation, Void> deleteDirectory(
            String storageAccount, String containerName, String path, boolean removeData);
}
