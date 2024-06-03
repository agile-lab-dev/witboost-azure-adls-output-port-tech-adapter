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
import com.azure.storage.file.datalake.models.DataLakeStorageException;
import it.agilelab.witboost.provisioning.adlsop.model.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.model.StorageAccountInfo;
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

        String url = "https://storage-account.dfs.core.windows.net/container/path/to/folder";

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

        String url = "https://storage-account.dfs.core.windows.net/container/path/to/folder";

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        verify(dataLakeFileSystemClient, never()).deleteDirectoryWithResponse("path/to/folder", true, null, null, null);

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", false);

        assertEquals(right(null), actualResult);
    }

    @Test
    void deleteDirectoryRemoveDataReturnsOk() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);

        String url = "https://storage-account.dfs.core.windows.net/container/path/to/folder";

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
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

        String url = "https://storage-account.dfs.core.windows.net/container/path/to/folder";

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
        when(dataLakeFileSystemClient.deleteDirectoryWithResponse("path/to/folder", true, null, null, null))
                .thenThrow(error);
        when(error.getErrorCode()).thenReturn("PathNotFound");

        var actualResult = adlsGen2Service.deleteDirectory("storage-account", "container", "path/to/folder", true);

        assertEquals(right(null), actualResult);
    }

    @Test
    void deleteDirectoryRemoveDataReturnsError() {
        DataLakeServiceClient dataLakeServiceClient = Mockito.mock(DataLakeServiceClient.class);
        DataLakeFileSystemClient dataLakeFileSystemClient = Mockito.mock(DataLakeFileSystemClient.class);
        DataLakeStorageException error = Mockito.mock(DataLakeStorageException.class);

        String url = "https://storage-account.dfs.core.windows.net/container/path/to/folder";

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
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

        String url = "https://storage-account.dfs.core.windows.net/container/path/to/folder";

        when(adlsGen2Service.getDataLakeServiceClient("storage-account")).thenReturn(dataLakeServiceClient);
        when(dataLakeServiceClient.getFileSystemClient("container")).thenReturn(dataLakeFileSystemClient);
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

    // Azure SDK doesn't perform any actual operations against the Azure env when creating the clients, so this is still
    // part of unit-test
    @Test
    void getDataLakeServiceClient() {
        assertDoesNotThrow(() -> adlsGen2Service.getDataLakeServiceClient("storage-account"));
    }
}
