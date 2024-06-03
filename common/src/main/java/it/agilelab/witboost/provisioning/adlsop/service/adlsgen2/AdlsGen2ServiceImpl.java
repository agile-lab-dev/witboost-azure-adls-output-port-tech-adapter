package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.azure.core.credential.TokenCredential;
import com.azure.resourcemanager.resourcegraph.ResourceGraphManager;
import com.azure.resourcemanager.resourcegraph.models.QueryRequest;
import com.azure.resourcemanager.resourcegraph.models.QueryResponse;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.model.StorageAccountInfo;
import it.agilelab.witboost.provisioning.adlsop.parser.Parser;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdlsGen2ServiceImpl implements AdlsGen2Service {

    private final TokenCredential tokenCredential;

    private final ResourceGraphManager resourceGraphManager;

    private static final String ADLS_STORAGE_ACCOUNT_URL = "https://%s.dfs.core.windows.net";
    private static final String RESOURCE_GRAPH_GET_STORAGE_ACCOUNT_INFO =
            "Resources | where type =~ 'Microsoft.Storage/storageAccounts' | where name == '%s'";

    private static final String STORAGE_BROWSER_URL_TEMPLATE = "https://portal.azure.com/#@/resource/%s/storagebrowser";
    private static final String PATH_NOT_FOUND = "PathNotFound";

    public AdlsGen2ServiceImpl(TokenCredential tokenCredential, ResourceGraphManager resourceGraphManager) {
        this.tokenCredential = tokenCredential;
        this.resourceGraphManager = resourceGraphManager;
    }

    @Override
    public Either<FailedOperation, Boolean> containerExists(String storageAccount, String containerName) {
        try {
            var dataLakeServiceClient = getDataLakeServiceClient(storageAccount);
            var dataLakeFileSystemClient = dataLakeServiceClient.getFileSystemClient(containerName);
            var response = dataLakeFileSystemClient.exists();
            log.info("Container {} in storage account {} exists?: {}", containerName, storageAccount, response);
            return right(response);
        } catch (Exception e) {
            log.error(
                    String.format(
                            "Error while checking the existence of the container '%s' in storage account '%s'",
                            containerName, storageAccount),
                    e);
            return Either.left(new FailedOperation(Collections.singletonList(new Problem(
                    getFailedMessage(
                            String.format(
                                    "Failed to check the existence of the container '%s' in storage account '%s'",
                                    containerName, storageAccount),
                            Optional.of(e)),
                    e))));
        }
    }

    @Override
    public Either<FailedOperation, AdlsGen2DirectoryInfo> createDirectory(
            String storageAccount, String containerName, String path) {
        try {
            log.info(
                    "Creating directory '{}' in container '{}' in storage account '{}'",
                    path,
                    containerName,
                    storageAccount);
            var dataLakeServiceClient = getDataLakeServiceClient(storageAccount);
            var dataLakeFileSystemClient = dataLakeServiceClient.getFileSystemClient(containerName);
            var directoryClient = dataLakeFileSystemClient.createDirectoryIfNotExists(path);
            boolean wasCreated = directoryClient.exists();
            log.info(
                    "Directory '{}' in container '{}' in storage account '{}' created?: {}",
                    path,
                    containerName,
                    storageAccount,
                    wasCreated);
            if (wasCreated) {
                var storageAccountInfo = getStorageAccountInfo(storageAccount);
                return storageAccountInfo.map(info -> {
                    log.info("Retrieved storage account information: {}", info);
                    var response = new AdlsGen2DirectoryInfo(
                            storageAccount,
                            containerName,
                            path,
                            directoryClient.getDirectoryUrl(),
                            getStorageBrowserUrl(info),
                            null);
                    return response;
                });
            }
            var error = String.format(
                    "Creation of directory '%s' on container '%s' in storage account '%s' failed",
                    path, containerName, storageAccount);
            log.error(error);
            return left(new FailedOperation(
                    Collections.singletonList(new Problem(getFailedMessage(error, Optional.empty())))));
        } catch (Exception e) {
            var error = String.format(
                    "Error while creating directory '%s' on container '%s' in storage account '%s'",
                    path, containerName, storageAccount);
            log.error(error, e);
            return Either.left(new FailedOperation(
                    Collections.singletonList(new Problem(getFailedMessage(error, Optional.of(e)), e))));
        }
    }

    @Override
    public Either<FailedOperation, Void> deleteDirectory(
            String storageAccount, String containerName, String path, boolean removeData) {
        try {
            log.info(
                    "Removing directory '{}' in container '{}' in storage account '{}'",
                    path,
                    containerName,
                    storageAccount);
            var dataLakeServiceClient = getDataLakeServiceClient(storageAccount);
            var dataLakeFileSystemClient = dataLakeServiceClient.getFileSystemClient(containerName);
            // TODO Remove ACL
            if (removeData) {
                log.info(
                        "removeData is true. Deleting directory '{}' in container '{}' in storage account '{}'",
                        path,
                        containerName,
                        storageAccount);
                dataLakeFileSystemClient.deleteDirectoryWithResponse(path, true, null, null, null);
            }
            return right(null);
        } catch (DataLakeStorageException e) {
            if (e.getErrorCode().equals(PATH_NOT_FOUND)) {
                log.info(
                        "Directory '{}' in container '{}' in storage account '{}' didn't exist in the first place",
                        path,
                        containerName,
                        storageAccount);
                return right(null);
            } else {
                var error = String.format(
                        "Error while deleting directory '%s' on container '%s' in storage account '%s'",
                        path, containerName, storageAccount);
                log.error(error, e);
                return Either.left(new FailedOperation(
                        Collections.singletonList(new Problem(getFailedMessage(error, Optional.of(e)), e))));
            }
        } catch (Exception e) {
            var error = String.format(
                    "Error while deleting directory '%s' on container '%s' in storage account '%s'",
                    path, containerName, storageAccount);
            log.error(error, e);
            return Either.left(new FailedOperation(
                    Collections.singletonList(new Problem(getFailedMessage(error, Optional.of(e)), e))));
        }
    }

    public DataLakeServiceClient getDataLakeServiceClient(String storageAccount) {
        return new DataLakeServiceClientBuilder()
                .endpoint(String.format(ADLS_STORAGE_ACCOUNT_URL, storageAccount))
                .credential(tokenCredential)
                .buildClient();
    }

    @Override
    public Either<FailedOperation, StorageAccountInfo> getStorageAccountInfo(String storageAccount) {
        QueryRequest queryRequest =
                new QueryRequest().withQuery(String.format(RESOURCE_GRAPH_GET_STORAGE_ACCOUNT_INFO, storageAccount));
        log.debug("Querying Resource Graph with Query \"{}\"", queryRequest.query());
        QueryResponse response = resourceGraphManager.resourceProviders().resources(queryRequest);
        log.debug("Received raw response: {}", response.data());
        return Parser.parseStorageAccountInfoList(response.data()).flatMap(l -> l.stream()
                .findFirst()
                .<Either<FailedOperation, StorageAccountInfo>>map(Either::right)
                .orElseGet(() -> {
                    var errorMessage = String.format(
                            "Storage Account '%s' doesn't exist. Resource Graph API returned empty response",
                            storageAccount);
                    log.error(errorMessage);
                    return Either.left(new FailedOperation(
                            Collections.singletonList(new Problem(getFailedMessage(errorMessage, Optional.empty())))));
                }));
    }

    public String removeTrailingLeadingSlash(String path) {
        String removed = path;
        if (removed.startsWith("/")) {
            removed = removed.substring(1);
        }
        if (removed.endsWith("/")) {
            removed = removed.substring(0, removed.length() - 1);
        }
        return removed;
    }

    public String getStorageBrowserUrl(StorageAccountInfo storageAccountInfo) {
        return String.format(STORAGE_BROWSER_URL_TEMPLATE, removeTrailingLeadingSlash(storageAccountInfo.getId()));
    }

    private String getFailedMessage(String baseMessage, Optional<Throwable> ex) {
        if (ex.isPresent()) {
            return String.format(
                    "%s. Please try again and if the issue persists contact the platform team. Details: %s",
                    baseMessage, ex.get().getMessage());
        }
        return String.format("%s. Please try again and if the issue persists contact the platform team", baseMessage);
    }
}
