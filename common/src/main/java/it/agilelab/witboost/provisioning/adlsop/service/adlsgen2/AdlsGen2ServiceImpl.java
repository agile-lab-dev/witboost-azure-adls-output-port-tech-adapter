package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdlsGen2ServiceImpl implements AdlsGen2Service {

    private final TokenCredential tokenCredential;

    private static final String ADLS_STORAGE_ACCOUNT_URL = "https://%s.dfs.core.windows.net";

    public AdlsGen2ServiceImpl(TokenCredential tokenCredential) {
        this.tokenCredential = tokenCredential;
    }

    @Override
    public Either<FailedOperation, Boolean> containerExists(String storageAccount, String containerName) {
        try {
            var dataLakeServiceClient = getDataLakeServiceClient(storageAccount);
            var dataLakeFileSystemClient = dataLakeServiceClient.getFileSystemClient(containerName);
            var response = dataLakeFileSystemClient.exists();
            log.info("Container {} in storage account {} exists?: {}", containerName, storageAccount, response);
            return Either.right(response);
        } catch (Exception e) {
            log.error(
                    String.format(
                            "Error while checking the existence of the container '%s' in storage account '%s'",
                            containerName, storageAccount),
                    e);
            return Either.left(new FailedOperation(Collections.singletonList(new Problem(
                    getFailedMessage("check the existence of", containerName, storageAccount, Optional.of(e)), e))));
        }
    }

    public DataLakeServiceClient getDataLakeServiceClient(String storageAccount) {
        return new DataLakeServiceClientBuilder()
                .endpoint(String.format(ADLS_STORAGE_ACCOUNT_URL, storageAccount))
                .credential(tokenCredential)
                .buildClient();
    }

    private String getFailedMessage(String operation, String storageAccount, String container, Optional<Throwable> ex) {
        if (ex.isPresent()) {
            return String.format(
                    "Failed to %s the container %s in storage account %s. Please try again and if the issue persists contact the platform team. Details: %s",
                    operation, container, storageAccount, ex.get().getMessage());
        }
        return String.format(
                "Failed to %s the container %s in storage account %s. Please try again and if the issue persists contact the platform team",
                operation, container, storageAccount);
    }
}
