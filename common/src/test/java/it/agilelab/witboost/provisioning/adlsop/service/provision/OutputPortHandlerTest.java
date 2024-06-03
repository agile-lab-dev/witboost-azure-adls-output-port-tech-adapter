package it.agilelab.witboost.provisioning.adlsop.service.provision;

import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutputPortHandlerTest {

    @Mock
    private AdlsGen2Service adlsGen2Service;

    @InjectMocks
    private OutputPortHandler outputPortHandler;

    @Test
    void testCreateOk() {
        OutputPortSpecific outputPortSpecific = new OutputPortSpecific();
        outputPortSpecific.setStorageAccount("storageAccount");
        outputPortSpecific.setContainer("containerName");
        outputPortSpecific.setPath("path");
        outputPortSpecific.setFileFormat("CSV");
        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setSpecific(outputPortSpecific);

        AdlsGen2DirectoryInfo directoryInfo = new AdlsGen2DirectoryInfo(
                "storageAccount",
                "containerName",
                "path",
                "https://storageAccount.dfs.core.windows.net/containerName/path",
                null,
                null);
        AdlsGen2DirectoryInfo directoryInfoComplete = new AdlsGen2DirectoryInfo(
                "storageAccount",
                "containerName",
                "path",
                "https://storageAccount.dfs.core.windows.net/containerName/path",
                null,
                "CSV");

        when(adlsGen2Service.createDirectory("storageAccount", "containerName", "path"))
                .thenReturn(right(directoryInfo));
        var actualRes = outputPortHandler.create(new ProvisionRequest<>(new DataProduct(), outputPort, true));

        assertTrue(actualRes.isRight());
        var actual = actualRes.get();
        assertEquals(actual, directoryInfoComplete);
    }

    @Test
    void testCreateWrongComponent() {
        StorageArea<Specific> storageArea = new StorageArea<>();
        storageArea.setKind("storagearea");
        storageArea.setId("storagearea-id");
        storageArea.setSpecific(new Specific());

        var actualRes = outputPortHandler.create(new ProvisionRequest<>(new DataProduct(), storageArea, true));

        var expectedError = "The component type is not of expected type OutputPort";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testCreateWrongSpecific() {
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setId("outputport-id");
        outputPort.setSpecific(new Specific());

        var actualRes = outputPortHandler.create(new ProvisionRequest<>(new DataProduct(), outputPort, true));

        var expectedError = "The specific section of the component outputport-id is not of type OutputPortSpecific";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testDestroyOk() {
        OutputPortSpecific outputPortSpecific = new OutputPortSpecific();
        outputPortSpecific.setStorageAccount("storageAccount");
        outputPortSpecific.setContainer("containerName");
        outputPortSpecific.setPath("path");
        outputPortSpecific.setFileFormat("CSV");
        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setSpecific(outputPortSpecific);

        when(adlsGen2Service.deleteDirectory("storageAccount", "containerName", "path", true))
                .thenReturn(right(null));
        var actualRes = outputPortHandler.destroy(new ProvisionRequest<>(new DataProduct(), outputPort, true));

        assertTrue(actualRes.isRight());
    }

    @Test
    void testDestroyWrongComponent() {
        StorageArea<Specific> storageArea = new StorageArea<>();
        storageArea.setKind("storagearea");
        storageArea.setId("storagearea-id");
        storageArea.setSpecific(new Specific());

        var actualRes = outputPortHandler.destroy(new ProvisionRequest<>(new DataProduct(), storageArea, true));

        var expectedError = "The component type is not of expected type OutputPort";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testDestroyWrongSpecific() {
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setId("outputport-id");
        outputPort.setSpecific(new Specific());

        var actualRes = outputPortHandler.destroy(new ProvisionRequest<>(new DataProduct(), outputPort, true));

        var expectedError = "The specific section of the component outputport-id is not of type OutputPortSpecific";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }
}
