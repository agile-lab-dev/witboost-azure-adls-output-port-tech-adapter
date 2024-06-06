package it.agilelab.witboost.provisioning.adlsop.service.provision;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureMapper;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2Service;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutputPortHandlerTest {

    @Mock
    private AdlsGen2Service adlsGen2Service;

    @Mock
    private AzureMapper azureMapper;

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

    @Test
    void testUpdateAclOk() {
        OutputPortSpecific outputPortSpecific = new OutputPortSpecific();
        outputPortSpecific.setStorageAccount("storageAccount");
        outputPortSpecific.setContainer("containerName");
        outputPortSpecific.setPath("path");
        outputPortSpecific.setFileFormat("CSV");
        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setSpecific(outputPortSpecific);

        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var mappedUsers = List.of("1234-5678-90ab-cdef", "1234-5678-90ab-cdef");
        Map<String, Either<Throwable, String>> mapResult = Map.of(
                users.get(0), right(mappedUsers.get(0)),
                users.get(1), right(mappedUsers.get(1)));
        when(azureMapper.map(Set.copyOf(users))).thenReturn(mapResult);
        when(adlsGen2Service.updateAcl("storageAccount", "containerName", "path", mappedUsers))
                .thenReturn(right(null));
        var actualRes = outputPortHandler.updateAcl(users, new ProvisionRequest<>(new DataProduct(), outputPort, true));

        assertTrue(actualRes.isRight());
    }

    @Test
    void testUpdateAclMappingError() {
        OutputPortSpecific outputPortSpecific = new OutputPortSpecific();
        outputPortSpecific.setStorageAccount("storageAccount");
        outputPortSpecific.setContainer("containerName");
        outputPortSpecific.setPath("path");
        outputPortSpecific.setFileFormat("CSV");
        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setSpecific(outputPortSpecific);

        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var mappedUsers = List.of("1234-5678-90ab-cdef");
        var error = new Throwable("Error!");
        Map<String, Either<Throwable, String>> mapResult = Map.of(
                users.get(0), right(mappedUsers.get(0)),
                users.get(1), left(error));

        when(azureMapper.map(Set.copyOf(users))).thenReturn(mapResult);
        when(adlsGen2Service.updateAcl("storageAccount", "containerName", "path", mappedUsers))
                .thenReturn(right(null));
        var actualRes = outputPortHandler.updateAcl(users, new ProvisionRequest<>(new DataProduct(), outputPort, true));

        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(error.getMessage(), p.description());
            assertTrue(p.cause().isPresent());
        });
    }

    @Test
    void testUpdateAclUpdateError() {
        OutputPortSpecific outputPortSpecific = new OutputPortSpecific();
        outputPortSpecific.setStorageAccount("storageAccount");
        outputPortSpecific.setContainer("containerName");
        outputPortSpecific.setPath("path");
        outputPortSpecific.setFileFormat("CSV");
        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setSpecific(outputPortSpecific);

        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var mappedUsers = List.of("1234-5678-90ab-cdef", "1234-5678-90ab-cdef");
        Map<String, Either<Throwable, String>> mapResult = Map.of(
                users.get(0), right(mappedUsers.get(0)),
                users.get(1), right(mappedUsers.get(1)));

        when(azureMapper.map(Set.copyOf(users))).thenReturn(mapResult);
        when(adlsGen2Service.updateAcl("storageAccount", "containerName", "path", mappedUsers))
                .thenReturn(left(new FailedOperation(Collections.singletonList(new Problem("Error!")))));
        var actualRes = outputPortHandler.updateAcl(users, new ProvisionRequest<>(new DataProduct(), outputPort, true));

        var expectedError = "Error!";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testUpdateAclWrongComponent() {
        StorageArea<Specific> storageArea = new StorageArea<>();
        storageArea.setKind("storagearea");
        storageArea.setId("storagearea-id");
        storageArea.setSpecific(new Specific());

        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");

        var actualRes =
                outputPortHandler.updateAcl(users, new ProvisionRequest<>(new DataProduct(), storageArea, true));

        var expectedError = "The component type is not of expected type OutputPort";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testUpdateAclWrongSpecific() {
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setId("outputport-id");
        outputPort.setSpecific(new Specific());

        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");

        var actualRes = outputPortHandler.updateAcl(users, new ProvisionRequest<>(new DataProduct(), outputPort, true));

        var expectedError = "The specific section of the component outputport-id is not of type OutputPortSpecific";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }
}
