package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.azure.core.credential.TokenCredential;
import com.azure.resourcemanager.resourcegraph.ResourceGraphManager;
import com.azure.resourcemanager.resourcegraph.models.QueryRequest;
import com.azure.resourcemanager.resourcegraph.models.QueryResponse;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.azure.storage.file.datalake.models.*;
import io.vavr.control.Either;
import io.vavr.control.Option;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.azure.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.model.azure.StorageAccountInfo;
import it.agilelab.witboost.provisioning.adlsop.parser.Parser;
import java.util.*;
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
                    return new AdlsGen2DirectoryInfo(
                            storageAccount,
                            containerName,
                            path,
                            directoryClient.getDirectoryUrl(),
                            getStorageBrowserUrl(info),
                            null);
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
            var directoryClient = dataLakeFileSystemClient.getDirectoryClient(path);

            log.info(
                    "Resetting ACLs in directory '{}' in container '{}' in storage account '{}'",
                    path,
                    containerName,
                    storageAccount);
            // Remove all ACLs, leaving the default owner, group and other entries
            var defaultPermissions = AccessControlUtils.getDefaultAccessControlEntries();
            var aclResult = directoryClient.setAccessControlRecursive(defaultPermissions);
            directoryClient.setAccessControlList(
                    defaultPermissions,
                    directoryClient.getAccessControl().getGroup(),
                    directoryClient.getAccessControl().getOwner());

            if (aclResult.getCounters().getFailedChangesCount() != 0) {
                log.error("Error resetting ACLs: {}", aclResult.getBatchFailures());
                return left(new FailedOperation(aclResult.getBatchFailures().stream()
                        .map(failure -> new Problem(failure.getErrorMessage()))
                        .toList()));
            }
            log.info("ACLs reset successful");

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

    /**
     * Performs the update ACL operation on a directory, giving execute (--x) permissions on the parent directories, and
     * read and execute (r-x) on the directory itself to the list of objectIds passed as parameter. It attempts all permissions
     * grants and then accumulates the results, returning a single FailedOperation in case of error
     * @param storageAccount Storage account name
     * @param containerName Container name
     * @param path Directory to grant r-x permissions
     * @param usersObjectId List of users objectIds to grant access
     * @return {@code Either.left(FailedOperation) } on failed attempt
     */
    public Either<FailedOperation, Void> updateAcl(
            String storageAccount, String containerName, String path, List<String> usersObjectId) {
        var subDirectories = path.split("/");
        String subPath = "";

        var dataLakeServiceClient = getDataLakeServiceClient(storageAccount);

        List<Either<FailedOperation, Void>> results = new ArrayList<>();
        for (String subDirectory : subDirectories) {
            RolePermissions userPermission = new RolePermissions();
            userPermission.setReadPermission(false).setWritePermission(false).setExecutePermission(true);
            var result = grantACL(
                    dataLakeServiceClient, containerName, subPath, usersObjectId, userPermission, false, false, false);
            results.add(result);
            subPath += subDirectory + "/";
        }
        RolePermissions userPermission = new RolePermissions();
        userPermission.setReadPermission(true).setWritePermission(false).setExecutePermission(true);
        var result =
                grantACL(dataLakeServiceClient, containerName, path, usersObjectId, userPermission, true, true, true);
        results.add(result);

        return FailedOperation.combineEither(right(null), results, (a, b) -> a);
    }

    /**
     * Grants permission on a single path of a container in a storage account for a list of users objectId
     * @param dataLakeServiceClient DataLakeServiceClient for the storage account, it is passed as argument to avoid authenticating for each single ACL request
     * @param containerName Container name
     * @param path Target directory
     * @param usersObjectId List of users objectIds to grant permission
     * @param userPermission Type of permission to be granted
     * @param grantRecursively Whether to grant the permission recursively on all child paths
     * @param addAsDefaultScope Whether to add the users as default ACL for new child objects of target directory
     * @param overridePermissions Whether to override the existing ACL permissions on target directory
     * @return {@code Either.left(FailedOperation) } on failed attempt
     */
    private Either<FailedOperation, Void> grantACL(
            DataLakeServiceClient dataLakeServiceClient,
            String containerName,
            String path,
            List<String> usersObjectId,
            RolePermissions userPermission,
            boolean grantRecursively,
            boolean addAsDefaultScope,
            boolean overridePermissions) {
        try {
            log.info(
                    "Granting ACL to {} on path {} on container {} on storage account {} with configs: grantRecursively={}, addAsDefaultScope={}, overridePermissions={}",
                    usersObjectId,
                    path,
                    containerName,
                    dataLakeServiceClient.getAccountName(),
                    grantRecursively,
                    addAsDefaultScope,
                    overridePermissions);

            var fsClient = dataLakeServiceClient.getFileSystemClient(containerName);
            // Azure doesn't like a / as initial character
            var directoryClient = fsClient.getDirectoryClient(removeTrailingLeadingSlash(path));

            ArrayList<PathAccessControlEntry> accessControlEntries = new ArrayList<>(usersObjectId.stream()
                    .flatMap(objectId -> {
                        ArrayList<PathAccessControlEntry> entries = new ArrayList<>();

                        PathAccessControlEntry userEntry = AccessControlUtils.buildPathAccessControlEntry(
                                userPermission, AccessControlType.USER, false, Option.of(objectId));
                        entries.add(userEntry);

                        if (addAsDefaultScope) {
                            PathAccessControlEntry defaultUserEntry = AccessControlUtils.buildPathAccessControlEntry(
                                    userPermission, AccessControlType.USER, true, Option.of(objectId));
                            entries.add(defaultUserEntry);
                        }
                        return entries.stream();
                    })
                    .toList());

            if (overridePermissions) {
                // If we override permissions we need to ensure owner user, group and other roles
                // are present on the ACL list
                accessControlEntries.addAll(AccessControlUtils.getDefaultAccessControlEntries());
            }

            if (grantRecursively) {
                AccessControlChangeResult result;
                if (overridePermissions) {
                    log.info("Overriding ACL recursively on path '{}' with entries {}", path, accessControlEntries);
                    result = directoryClient.setAccessControlRecursive(accessControlEntries);
                } else {
                    log.info("Updating ACL recursively on path '{}' with entries {}", path, accessControlEntries);
                    result = directoryClient.updateAccessControlRecursive(accessControlEntries);
                }

                if (result.getCounters().getFailedChangesCount() == 0) {
                    return right(null);
                } else {
                    return left(new FailedOperation(result.getBatchFailures().stream()
                            .map(failure -> new Problem(failure.getErrorMessage()))
                            .toList()));
                }
            } else {
                if (overridePermissions) {
                    log.info("Overriding ACL non-recursively on path '{}' with entries {}", path, accessControlEntries);
                    directoryClient.setAccessControlList(
                            accessControlEntries,
                            directoryClient.getAccessControl().getGroup(),
                            directoryClient.getAccessControl().getOwner());

                } else {
                    // SDK doesn't provide a way to update non-recursively, so we have to get the current ACL and
                    // compare it with the one we are using, updating the entries in common and adding the missing ones.
                    // Note that this doesn't remove usersObjectId that are not in our ACL anymore, as we cannot be sure
                    // that they aren't managed by another component/provisioner
                    List<PathAccessControlEntry> pathAccessControlEntries =
                            directoryClient.getAccessControl().getAccessControlList();

                    accessControlEntries.forEach(entry -> pathAccessControlEntries.stream()
                            .filter(allEntry -> allEntry.getEntityId() != null
                                    && allEntry.getEntityId().equals(entry.getEntityId()))
                            .findFirst()
                            .ifPresentOrElse(
                                    pathAccessControlEntry -> {
                                        var idx = pathAccessControlEntries.indexOf(pathAccessControlEntry);
                                        pathAccessControlEntries.set(idx, entry);
                                    },
                                    () -> pathAccessControlEntries.add(entry)));

                    log.info(
                            "Updating ACL non-recursively on path '{}' with entries {}",
                            path,
                            pathAccessControlEntries);
                    directoryClient.setAccessControlList(
                            pathAccessControlEntries,
                            directoryClient.getAccessControl().getGroup(),
                            directoryClient.getAccessControl().getOwner());
                }
                return right(null);
            }
        } catch (Exception e) {
            log.error(
                    String.format(
                            "Error while updating the ACLs on the path '%s' of container '%s' in storage account '%s'",
                            path, containerName, dataLakeServiceClient.getAccountName()),
                    e);
            return Either.left(new FailedOperation(Collections.singletonList(new Problem(
                    getFailedMessage(
                            String.format(
                                    "Failed to update the ACLs on path '%s' in container '%s' in storage account '%s'",
                                    path, containerName, dataLakeServiceClient.getAccountName()),
                            Optional.of(e)),
                    e))));
        }
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
