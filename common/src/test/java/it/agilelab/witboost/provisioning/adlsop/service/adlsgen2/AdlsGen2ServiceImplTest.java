package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.azure.core.credential.TokenCredential;
import com.azure.resourcemanager.resourcegraph.ResourceGraphManager;
import com.azure.resourcemanager.resourcegraph.models.QueryRequest;
import com.azure.resourcemanager.resourcegraph.models.QueryResponse;
import com.azure.resourcemanager.resourcegraph.models.ResourceProviders;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.models.*;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.azure.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.model.azure.StorageAccountInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdlsGen2ServiceImplTest {

    @Mock
    TokenCredential tokenCredential;

    @Mock
    ResourceGraphManager resourceGraphManager;

    @InjectMocks
    @Spy
    AdlsGen2ServiceImpl adlsGen2Service;

    @Test
    void containerExistsReturnsTrue() {

        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.exists()).thenReturn(true);

        var response = adlsGen2Service.containerExists("storage-account", "container");
        assertTrue(response.isRight());
        assertTrue(response.get());
    }

    @Test
    void containerExistsReturnsFalse() {

        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.exists()).thenReturn(false);

        var response = adlsGen2Service.containerExists("storage-account", "container");
        assertTrue(response.isRight());
        assertFalse(response.get());
    }

    @Test
    void containerExistsReturnsError() {

        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);

        var expectedDesc =
                "Failed to check the existence of the container 'container' in storage account 'storage-account'. Please try again and if the issue persists contact the platform team. Details: Error!";

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.exists()).thenThrow(new RuntimeException("Error!"));

        var actualResult = adlsGen2Service.containerExists("storage-account", "container");

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(1, actualResult.getLeft().problems().size());
        actualResult.getLeft().problems().forEach(p -> {
            Assertions.assertEquals(expectedDesc, p.description());
            Assertions.assertTrue(p.cause().isPresent());
        });
    }

    @Test
    void getStorageBrowserOk() {
        String fullId =
                "subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account";
        StorageAccountInfo storageAccountInfo = new StorageAccountInfo(fullId, null, null, null, null, null);
        String expectedUrl =
                "https://portal.azure.com/#@/resource/subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account/storagebrowser";
        var actualRes = adlsGen2Service.getStorageBrowserUrl(storageAccountInfo);
        assertEquals(expectedUrl, actualRes);
    }

    @Test
    void getStorageAccountInfoReturnsError() {
        String storageAccount = "inexistent-storage-account";

        QueryResponse queryResponse = Mockito.mock(QueryResponse.class);
        ResourceProviders resourceProviders = Mockito.mock(ResourceProviders.class);
        when(resourceGraphManager.resourceProviders()).thenReturn(resourceProviders);
        when(resourceProviders.resources(any(QueryRequest.class))).thenReturn(queryResponse);
        when(queryResponse.data()).thenReturn(List.of());

        var actualResult = adlsGen2Service.getStorageAccountInfo(storageAccount);
        var expectedDesc =
                "Storage Account 'inexistent-storage-account' doesn't exist. Resource Graph API returned empty response. Please try again and if the issue persists contact the platform team";

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(1, actualResult.getLeft().problems().size());
        actualResult.getLeft().problems().forEach(p -> {
            Assertions.assertEquals(expectedDesc, p.description());
        });
    }

    @Test
    void createDirectoryReturnsOk() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient dataLakeDirectoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        QueryResponse queryResponse = Mockito.mock(QueryResponse.class);
        ResourceProviders resourceProviders = Mockito.mock(ResourceProviders.class);

        var storageAccountInfoList = List.of(new StorageAccountInfo(
                "/subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account",
                null,
                null,
                null,
                null,
                null));
        String url = "https://storage-account.dfs.core.windows.net/container/path/to/folder";
        String storageBrowserUrl =
                "https://portal.azure.com/#@/resource/subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account/storagebrowser";

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.createDirectoryIfNotExists("path/to/folder"))
                .thenReturn(dataLakeDirectoryClient);
        when(dataLakeDirectoryClient.exists()).thenReturn(true);
        when(dataLakeDirectoryClient.getDirectoryUrl()).thenReturn(url);

        when(resourceGraphManager.resourceProviders()).thenReturn(resourceProviders);
        when(resourceProviders.resources(any(QueryRequest.class))).thenReturn(queryResponse);
        when(queryResponse.data()).thenReturn(storageAccountInfoList);

        AdlsGen2DirectoryInfo expectedResult = new AdlsGen2DirectoryInfo(
                "storage-account", "container", "path/to/folder", url, storageBrowserUrl, null);

        var actualResult = adlsGen2Service.createDirectory("storage-account", "container", "path/to/folder");

        assertEquals(right(expectedResult), actualResult);
    }

    @Test
    void createDirectoryFailsIfDirDoesntExistAfterCreation() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient dataLakeDirectoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.createDirectoryIfNotExists("path/to/folder"))
                .thenReturn(dataLakeDirectoryClient);
        when(dataLakeDirectoryClient.exists()).thenReturn(false);

        var actualResult = adlsGen2Service.createDirectory("storage-account", "container", "path/to/folder");

        var expectedDesc =
                "Creation of directory 'path/to/folder' on container 'container' in storage account 'storage-account' failed. Please try again and if the issue persists contact the platform team";

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(1, actualResult.getLeft().problems().size());
        actualResult.getLeft().problems().forEach(p -> {
            Assertions.assertEquals(expectedDesc, p.description());
            Assertions.assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void createDirectoryReturnsError() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.createDirectoryIfNotExists("path/to/folder"))
                .thenThrow(new RuntimeException("Error!"));

        var actualResult = adlsGen2Service.createDirectory("storage-account", "container", "path/to/folder");

        var expectedDesc =
                "Error while creating directory 'path/to/folder' on container 'container' in storage account 'storage-account'. Please try again and if the issue persists contact the platform team. Details: Error!";

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(1, actualResult.getLeft().problems().size());
        actualResult.getLeft().problems().forEach(p -> {
            Assertions.assertEquals(expectedDesc, p.description());
            Assertions.assertTrue(p.cause().isPresent());
        });
    }

    @Test
    void deleteDirectoryNoRemoveDataReturnsOk() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient directoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult();
        accessControlChangeResult.setCounters(new AccessControlChangeCounters().setFailedChangesCount(0));

        PathAccessControl pathAccessControl = new PathAccessControl(null, null, "group", "owner");

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.getDirectoryClient("path/to/folder")).thenReturn(directoryClient);

        when(directoryClient.getAccessControl()).thenReturn(pathAccessControl);
        when(directoryClient.setAccessControlList(
                        AccessControlUtils.getDefaultAccessControlEntries(), "group", "owner"))
                .thenReturn(new PathInfo(null, null));
        when(directoryClient.setAccessControlRecursive(AccessControlUtils.getDefaultAccessControlEntries()))
                .thenReturn(accessControlChangeResult);
        verify(dataLakeFileSystemClient, never()).deleteDirectoryWithResponse("path/to/folder", true, null, null, null);

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", false);

        assertEquals(right(null), actualResult);
    }

    @Test
    void deleteDirectoryRemoveDataReturnsOk() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient directoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult();
        accessControlChangeResult.setCounters(new AccessControlChangeCounters().setFailedChangesCount(0));

        PathAccessControl pathAccessControl = new PathAccessControl(null, null, "group", "owner");

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.getDirectoryClient("path/to/folder")).thenReturn(directoryClient);

        when(directoryClient.getAccessControl()).thenReturn(pathAccessControl);
        when(directoryClient.setAccessControlList(
                        AccessControlUtils.getDefaultAccessControlEntries(), "group", "owner"))
                .thenReturn(new PathInfo(null, null));
        when(directoryClient.setAccessControlRecursive(AccessControlUtils.getDefaultAccessControlEntries()))
                .thenReturn(accessControlChangeResult);
        when(dataLakeFileSystemClient.deleteDirectoryWithResponse("path/to/folder", true, null, null, null))
                .thenReturn(null);

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", true);

        assertEquals(right(null), actualResult);
    }

    @Test
    void deleteInexistentDirectoryRemoveDataReturnsOk() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeStorageException error = Mockito.mock(DataLakeStorageException.class);
        DataLakeDirectoryClient directoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult();
        accessControlChangeResult.setCounters(new AccessControlChangeCounters().setFailedChangesCount(0));

        PathAccessControl pathAccessControl = new PathAccessControl(null, null, "group", "owner");

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.getDirectoryClient("path/to/folder")).thenReturn(directoryClient);

        when(directoryClient.getAccessControl()).thenReturn(pathAccessControl);
        when(directoryClient.setAccessControlList(
                        AccessControlUtils.getDefaultAccessControlEntries(), "group", "owner"))
                .thenReturn(new PathInfo(null, null));
        when(directoryClient.setAccessControlRecursive(AccessControlUtils.getDefaultAccessControlEntries()))
                .thenReturn(accessControlChangeResult);
        when(dataLakeFileSystemClient.deleteDirectoryWithResponse("path/to/folder", true, null, null, null))
                .thenThrow(error);
        when(error.getErrorCode()).thenReturn("PathNotFound");

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", true);

        assertEquals(right(null), actualResult);
    }

    @Test
    void deleteDirectoryReturnsAclError() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient directoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult()
                .setCounters(new AccessControlChangeCounters().setFailedChangesCount(2))
                .setBatchFailures(List.of(
                        new AccessControlChangeFailure().setErrorMessage("Error 1"),
                        new AccessControlChangeFailure().setErrorMessage("Error 2")));

        PathAccessControl pathAccessControl = new PathAccessControl(null, null, "group", "owner");

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.getDirectoryClient("path/to/folder")).thenReturn(directoryClient);

        when(directoryClient.getAccessControl()).thenReturn(pathAccessControl);
        when(directoryClient.setAccessControlList(
                        AccessControlUtils.getDefaultAccessControlEntries(), "group", "owner"))
                .thenReturn(new PathInfo(null, null));
        when(directoryClient.setAccessControlRecursive(AccessControlUtils.getDefaultAccessControlEntries()))
                .thenReturn(accessControlChangeResult);
        verify(dataLakeFileSystemClient, never()).deleteDirectoryWithResponse("path/to/folder", true, null, null, null);

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", true);

        var expectedDesc = List.of("Error 1", "Error 2");

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(2, actualResult.getLeft().problems().size());
        Assertions.assertEquals(
                expectedDesc,
                actualResult.getLeft().problems().stream()
                        .map(Problem::description)
                        .toList());
    }

    @Test
    void deleteDirectoryRemoveDataReturnsError() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeStorageException error = Mockito.mock(DataLakeStorageException.class);
        DataLakeDirectoryClient directoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult();
        accessControlChangeResult.setCounters(new AccessControlChangeCounters().setFailedChangesCount(0));

        PathAccessControl pathAccessControl = new PathAccessControl(null, null, "group", "owner");

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.getDirectoryClient("path/to/folder")).thenReturn(directoryClient);

        when(directoryClient.getAccessControl()).thenReturn(pathAccessControl);
        when(directoryClient.setAccessControlList(
                        AccessControlUtils.getDefaultAccessControlEntries(), "group", "owner"))
                .thenReturn(new PathInfo(null, null));
        when(directoryClient.setAccessControlRecursive(AccessControlUtils.getDefaultAccessControlEntries()))
                .thenReturn(accessControlChangeResult);
        when(dataLakeFileSystemClient.deleteDirectoryWithResponse("path/to/folder", true, null, null, null))
                .thenThrow(error);
        when(error.getErrorCode()).thenReturn("AuthorizationFailed");
        when(error.getMessage()).thenReturn("Error!");

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", true);

        var expectedDesc =
                "Error while deleting directory 'path/to/folder' on container 'container' in storage account 'storage-account'. Please try again and if the issue persists contact the platform team. Details: Error!";

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(1, actualResult.getLeft().problems().size());
        actualResult.getLeft().problems().forEach(p -> {
            Assertions.assertEquals(expectedDesc, p.description());
            Assertions.assertTrue(p.cause().isPresent());
        });
    }

    @Test
    void deleteDirectoryRemoveDataReturnsGenericException() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient directoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult();
        accessControlChangeResult.setCounters(new AccessControlChangeCounters().setFailedChangesCount(0));

        PathAccessControl pathAccessControl = new PathAccessControl(null, null, "group", "owner");

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.getDirectoryClient("path/to/folder")).thenReturn(directoryClient);

        when(directoryClient.getAccessControl()).thenReturn(pathAccessControl);
        when(directoryClient.setAccessControlList(
                        AccessControlUtils.getDefaultAccessControlEntries(), "group", "owner"))
                .thenReturn(new PathInfo(null, null));
        when(directoryClient.setAccessControlRecursive(AccessControlUtils.getDefaultAccessControlEntries()))
                .thenReturn(accessControlChangeResult);
        when(dataLakeFileSystemClient.deleteDirectoryWithResponse("path/to/folder", true, null, null, null))
                .thenThrow(new RuntimeException("Error!"));

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", true);

        var expectedDesc =
                "Error while deleting directory 'path/to/folder' on container 'container' in storage account 'storage-account'. Please try again and if the issue persists contact the platform team. Details: Error!";

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(1, actualResult.getLeft().problems().size());
        actualResult.getLeft().problems().forEach(p -> {
            Assertions.assertEquals(expectedDesc, p.description());
            Assertions.assertTrue(p.cause().isPresent());
        });
    }

    @Test
    void updateAclReturnsOk() {
        var users = List.of("1234-abcd", "5678-90ef");

        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient parentDirectoryClient = Mockito.mock(DataLakeDirectoryClient.class);
        DataLakeDirectoryClient childDirectoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getAccountName()).thenReturn("storage-account");
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        lenient().when(dataLakeFileSystemClient.getDirectoryClient("")).thenReturn(parentDirectoryClient);
        lenient()
                .when(dataLakeFileSystemClient.getDirectoryClient("parentFolder"))
                .thenReturn(parentDirectoryClient);
        lenient()
                .when(dataLakeFileSystemClient.getDirectoryClient("parentFolder/childFolder"))
                .thenReturn(childDirectoryClient);

        // Update parents with --x permissions
        ArrayList<PathAccessControlEntry> parentEntries =
                new ArrayList<>(AccessControlUtils.getDefaultAccessControlEntries());
        parentEntries.add(PathAccessControlEntry.parse("user:1234-abcd:--x"));
        PathAccessControl parentACL = new PathAccessControl(parentEntries, null, "group", "owner");

        ArrayList<PathAccessControlEntry> parentUpdatedEntries = new ArrayList<>(parentEntries);
        parentUpdatedEntries.add(PathAccessControlEntry.parse("user:5678-90ef:--x"));

        when(parentDirectoryClient.getAccessControl()).thenReturn(parentACL);
        when(parentDirectoryClient.setAccessControlList(parentUpdatedEntries, "group", "owner"))
                .thenReturn(null);

        // Update target child with r-x permissions
        ArrayList<PathAccessControlEntry> childEntries = new ArrayList<>();
        childEntries.add(PathAccessControlEntry.parse("user:1234-abcd:r-x"));
        childEntries.add(PathAccessControlEntry.parse("default:user:1234-abcd:r-x"));
        childEntries.add(PathAccessControlEntry.parse("user:5678-90ef:r-x"));
        childEntries.add(PathAccessControlEntry.parse("default:user:5678-90ef:r-x"));
        childEntries.addAll(AccessControlUtils.getDefaultAccessControlEntries());

        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult();
        accessControlChangeResult.setCounters(new AccessControlChangeCounters().setFailedChangesCount(0));

        when(childDirectoryClient.setAccessControlRecursive(childEntries)).thenReturn(accessControlChangeResult);

        var actual = adlsGen2Service.updateAcl("storage-account", "container", "parentFolder/childFolder", users);

        assertTrue(actual.isRight());
    }

    @Test
    void updateAclFailsGrantRecursivelyOnTargetDirectory() {
        var users = List.of("1234-abcd", "5678-90ef");

        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeDirectoryClient parentDirectoryClient = Mockito.mock(DataLakeDirectoryClient.class);
        DataLakeDirectoryClient childDirectoryClient = Mockito.mock(DataLakeDirectoryClient.class);

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getAccountName()).thenReturn("storage-account");
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        lenient().when(dataLakeFileSystemClient.getDirectoryClient("")).thenReturn(parentDirectoryClient);
        lenient()
                .when(dataLakeFileSystemClient.getDirectoryClient("parentFolder"))
                .thenReturn(parentDirectoryClient);
        lenient()
                .when(dataLakeFileSystemClient.getDirectoryClient("parentFolder/childFolder"))
                .thenReturn(childDirectoryClient);

        // Update parents with --x permissions
        ArrayList<PathAccessControlEntry> parentEntries =
                new ArrayList<>(AccessControlUtils.getDefaultAccessControlEntries());
        parentEntries.add(PathAccessControlEntry.parse("user:1234-abcd:--x"));
        PathAccessControl parentACL = new PathAccessControl(parentEntries, null, "group", "owner");

        ArrayList<PathAccessControlEntry> parentUpdatedEntries = new ArrayList<>(parentEntries);
        parentUpdatedEntries.add(PathAccessControlEntry.parse("user:5678-90ef:--x"));

        when(parentDirectoryClient.getAccessControl()).thenReturn(parentACL);
        when(parentDirectoryClient.setAccessControlList(parentUpdatedEntries, "group", "owner"))
                .thenReturn(null);

        // Update target child with r-x permissions
        ArrayList<PathAccessControlEntry> childEntries = new ArrayList<>();
        childEntries.add(PathAccessControlEntry.parse("user:1234-abcd:r-x"));
        childEntries.add(PathAccessControlEntry.parse("default:user:1234-abcd:r-x"));
        childEntries.add(PathAccessControlEntry.parse("user:5678-90ef:r-x"));
        childEntries.add(PathAccessControlEntry.parse("default:user:5678-90ef:r-x"));
        childEntries.addAll(AccessControlUtils.getDefaultAccessControlEntries());

        String expectedDesc = "Error!";
        AccessControlChangeResult accessControlChangeResult = new AccessControlChangeResult();
        accessControlChangeResult
                .setCounters(new AccessControlChangeCounters().setFailedChangesCount(1))
                .setBatchFailures(List.of(new AccessControlChangeFailure().setErrorMessage(expectedDesc)));

        when(childDirectoryClient.setAccessControlRecursive(childEntries)).thenReturn(accessControlChangeResult);

        var actualResult = adlsGen2Service.updateAcl("storage-account", "container", "parentFolder/childFolder", users);

        Assertions.assertTrue(actualResult.isLeft());
        Assertions.assertEquals(1, actualResult.getLeft().problems().size());
        actualResult.getLeft().problems().forEach(p -> {
            Assertions.assertEquals(expectedDesc, p.description());
            Assertions.assertTrue(p.cause().isEmpty());
        });
    }

    // Azure SDK doesn't perform any actual operations against the Azure env when creating the clients, so this is still
    // part of unit-test
    @Test
    void getDataLakeServiceClient() {
        assertDoesNotThrow(() -> adlsGen2Service.getDataLakeServiceClient("storage-account"));
    }
}
