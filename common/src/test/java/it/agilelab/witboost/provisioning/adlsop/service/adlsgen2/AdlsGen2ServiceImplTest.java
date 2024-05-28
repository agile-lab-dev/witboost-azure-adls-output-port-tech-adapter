package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
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
                "Failed to check the existence of the container storage-account in storage account container. Please try again and if the issue persists contact the platform team. Details: Error!";

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

    // Azure SDK doesn't perform any actual operations against the Azure env when creating the clients, so this is still
    // part of unit-test
    @Test
    void getDataLakeServiceClient() {
        assertDoesNotThrow(() -> adlsGen2Service.getDataLakeServiceClient("storage-account"));
    }
}
