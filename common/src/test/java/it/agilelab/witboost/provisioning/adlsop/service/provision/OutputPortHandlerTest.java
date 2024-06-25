package it.agilelab.witboost.provisioning.adlsop.service.provision;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.model.azure.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureMapper;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2Service;
import java.util.*;
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

    private final OutputPort<OutputPortSpecific> outputPort;
    private final OutputPort<OutputPortSpecific> outputPortNoDepends;
    private final StorageArea<Specific> storageArea;
    private final StorageArea<Specific> storageAreaNoInfo;
    private final DataProduct dataProduct;

    ObjectMapper om = new ObjectMapper();

    public OutputPortHandlerTest() {
        om.registerModule(new Jdk8Module());

        OutputPortSpecific specific = new OutputPortSpecific();
        specific.setContainer("containerName");
        specific.setPath("path");
        specific.setFileFormat("CSV");

        outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setId("urn:dmb:cmp:healthcare:vaccinations:0:outputport");
        outputPort.setDependsOn(List.of("urn:dmb:cmp:healthcare:vaccinations:0:storage"));
        outputPort.setSpecific(specific);

        StorageDeployInfo storageDeployInfo = new StorageDeployInfo("storageAccount");
        storageArea = new StorageArea<>();
        storageArea.setId("urn:dmb:cmp:healthcare:vaccinations:0:storage");
        storageArea.setName("storage name");
        storageArea.setDescription("storage desc");
        storageArea.setKind("storage");
        storageArea.setSpecific(new Specific());
        storageArea.setInfo(Optional.of(om.valueToTree(storageDeployInfo)));

        outputPortNoDepends = new OutputPort<>();
        outputPortNoDepends.setKind("outputport");
        outputPortNoDepends.setId("urn:dmb:cmp:healthcare:vaccinations:0:outputport2");
        outputPortNoDepends.setDependsOn(Collections.emptyList());
        outputPortNoDepends.setSpecific(specific);

        storageAreaNoInfo = new StorageArea<>();
        storageAreaNoInfo.setId("urn:dmb:cmp:healthcare:vaccinations:0:storage");
        storageAreaNoInfo.setName("storage name");
        storageAreaNoInfo.setDescription("storage desc");
        storageAreaNoInfo.setKind("storage");
        storageAreaNoInfo.setSpecific(new Specific());

        var storageAreaNode = om.valueToTree(storageArea);
        var outputPortNode = om.valueToTree(outputPort);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(storageAreaNode);
        nodes.add(outputPortNode);
        dataProduct = new DataProduct();
        dataProduct.setComponents(nodes);
    }

    @Test
    void testCreateOk() {

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
        var actualRes = outputPortHandler.create(new ProvisionRequest<>(dataProduct, outputPort, true));

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
        outputPort.setDependsOn(List.of("a-storage-id"));
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
    void testCreateMissingDependsOn() {
        var actualRes = outputPortHandler.create(new ProvisionRequest<>(new DataProduct(), outputPortNoDepends, true));

        var expectedError = "The output port has not a corresponding dependent storage area";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testCreateWronglyMissingStorageDeployInfo() {
        var outputPortNode = om.valueToTree(outputPort);
        var storageAreaNode = om.valueToTree(storageAreaNoInfo);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(storageAreaNode);
        nodes.add(outputPortNode);
        DataProduct dp = new DataProduct();
        dp.setComponents(nodes);

        var actualRes = outputPortHandler.create(new ProvisionRequest<>(dp, outputPort, true));

        var error = new FailedOperation(Collections.singletonList(new Problem(
                "Failed retrieving deploy info from component urn:dmb:cmp:healthcare:vaccinations:0:storage")));

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(actualRes.getLeft(), error);
    }

    @Test
    void testDestroyOk() {
        when(adlsGen2Service.deleteDirectory("storageAccount", "containerName", "path", true))
                .thenReturn(right(null));
        var actualRes = outputPortHandler.destroy(new ProvisionRequest<>(dataProduct, outputPort, true));

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
        outputPort.setDependsOn(List.of("a-storage-id"));
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
    void testDestroyMissingDependsOn() {
        var actualRes = outputPortHandler.destroy(new ProvisionRequest<>(new DataProduct(), outputPortNoDepends, true));

        var expectedError = "The output port has not a corresponding dependent storage area";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testDestroyWronglyMissingStorageDeployInfo() {
        var outputPortNode = om.valueToTree(outputPort);
        var storageAreaNode = om.valueToTree(storageAreaNoInfo);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(storageAreaNode);
        nodes.add(outputPortNode);
        DataProduct dp = new DataProduct();
        dp.setComponents(nodes);

        var actualRes = outputPortHandler.destroy(new ProvisionRequest<>(dp, outputPort, true));

        var error = new FailedOperation(Collections.singletonList(new Problem(
                "Failed retrieving deploy info from component urn:dmb:cmp:healthcare:vaccinations:0:storage")));

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(actualRes.getLeft(), error);
    }

    @Test
    void testUpdateAclOk() {
        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var mappedUsers = List.of("1234-5678-90ab-cdef", "1234-5678-90ab-cdef");
        var storageInfo = new ProvisioningResult("storageAccount");

        Map<String, Either<Throwable, String>> mapResult = Map.of(
                users.get(0), right(mappedUsers.get(0)),
                users.get(1), right(mappedUsers.get(1)));
        when(azureMapper.map(Set.copyOf(users))).thenReturn(mapResult);
        when(adlsGen2Service.updateAcl("storageAccount", "containerName", "path", mappedUsers))
                .thenReturn(right(null));
        var actualRes =
                outputPortHandler.updateAcl(users, new ProvisionRequest<>(dataProduct, outputPort, true), storageInfo);

        assertTrue(actualRes.isRight());
    }

    @Test
    void testUpdateAclMappingError() {
        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var mappedUsers = List.of("1234-5678-90ab-cdef");
        var error = new Throwable("Error!");
        Map<String, Either<Throwable, String>> mapResult = Map.of(
                users.get(0), right(mappedUsers.get(0)),
                users.get(1), left(error));
        var storageInfo = new ProvisioningResult("storageAccount");

        when(azureMapper.map(Set.copyOf(users))).thenReturn(mapResult);
        when(adlsGen2Service.updateAcl("storageAccount", "containerName", "path", mappedUsers))
                .thenReturn(right(null));
        var actualRes =
                outputPortHandler.updateAcl(users, new ProvisionRequest<>(dataProduct, outputPort, true), storageInfo);

        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(error.getMessage(), p.description());
            assertTrue(p.cause().isPresent());
        });
    }

    @Test
    void testUpdateAclUpdateError() {
        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var mappedUsers = List.of("1234-5678-90ab-cdef", "1234-5678-90ab-cdef");
        Map<String, Either<Throwable, String>> mapResult = Map.of(
                users.get(0), right(mappedUsers.get(0)),
                users.get(1), right(mappedUsers.get(1)));
        var storageInfo = new ProvisioningResult("storageAccount");

        when(azureMapper.map(Set.copyOf(users))).thenReturn(mapResult);
        when(adlsGen2Service.updateAcl("storageAccount", "containerName", "path", mappedUsers))
                .thenReturn(left(new FailedOperation(Collections.singletonList(new Problem("Error!")))));
        var actualRes =
                outputPortHandler.updateAcl(users, new ProvisionRequest<>(dataProduct, outputPort, true), storageInfo);

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
        var storageInfo = new ProvisioningResult("storageAccount");

        var actualRes = outputPortHandler.updateAcl(
                users, new ProvisionRequest<>(new DataProduct(), storageArea, true), storageInfo);

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
        outputPort.setDependsOn(List.of("a-storage-id"));
        outputPort.setSpecific(new Specific());

        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var storageInfo = new ProvisioningResult("storageAccount");

        var actualRes = outputPortHandler.updateAcl(
                users, new ProvisionRequest<>(new DataProduct(), outputPort, true), storageInfo);

        var expectedError = "The specific section of the component outputport-id is not of type OutputPortSpecific";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testUpdateAclWronglyMissingStorageDeployInfo() {
        var outputPortNode = om.valueToTree(outputPort);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(outputPortNode);
        DataProduct dp = new DataProduct();
        dp.setComponents(nodes);

        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        var storageInfo = new ProvisioningResult((String) null);
        var actualRes = outputPortHandler.updateAcl(users, new ProvisionRequest<>(dp, outputPort, true), storageInfo);

        var error = new FailedOperation(Collections.singletonList(
                new Problem("Failed retrieving Storage Account name from deploy private info")));

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(error, actualRes.getLeft());
    }
}
