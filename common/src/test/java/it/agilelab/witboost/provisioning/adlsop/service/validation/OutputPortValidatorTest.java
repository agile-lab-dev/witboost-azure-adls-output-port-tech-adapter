package it.agilelab.witboost.provisioning.adlsop.service.validation;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2ServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {OutputPortValidator.class, ValidationAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
public class OutputPortValidatorTest {

    @MockBean
    AdlsGen2ServiceImpl adlsGen2Service;

    @Autowired
    OutputPortValidator outputPortValidator;

    private final OutputPort<OutputPortSpecific> outputPort;
    private final OutputPort<OutputPortSpecific> outputPortNoDepends;
    private final StorageArea<Specific> storageArea;
    private final StorageArea<Specific> storageAreaNoInfo;
    private final DataProduct dataProduct;

    private final ObjectMapper om = new ObjectMapper();

    public OutputPortValidatorTest() {
        om.registerModule(new Jdk8Module());

        OutputPortSpecific specific = new OutputPortSpecific();
        specific.setContainer("container");
        specific.setPath("path");
        specific.setFileFormat("CSV");

        outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        outputPort.setId("urn:dmb:cmp:healthcare:vaccinations:0:outputport");
        outputPort.setName("output port name");
        outputPort.setDescription("output port desc");
        outputPort.setDependsOn(List.of("urn:dmb:cmp:healthcare:vaccinations:0:storage"));
        outputPort.setSpecific(specific);

        StorageDeployInfo storageDeployInfo = new StorageDeployInfo("storage-account");
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
        outputPortNoDepends.setName("output port name");
        outputPortNoDepends.setDescription("output port desc");
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
    public void testValidateNoStorageAccountOk() {
        var actualRes = outputPortValidator.validate(dataProduct, outputPort, false);
        assertTrue(actualRes.isRight());
    }

    @Test
    public void testValidateWithStorageAccountOk() {
        when(adlsGen2Service.containerExists("storage-account", "container")).thenReturn(right(true));

        var actualRes = outputPortValidator.validate(dataProduct, outputPort, true);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testValidateWithStorageAccountNotFound() {
        when(adlsGen2Service.containerExists("storage-account", "container")).thenReturn(right(false));

        String expectedDesc = "The container 'container' on storage account 'storage-account' doesn't exist";

        var actualRes = outputPortValidator.validate(dataProduct, outputPort, true);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    public void testValidateWithStorageAccountError() {
        var error = new FailedOperation(Collections.singletonList(new Problem("Error")));

        when(adlsGen2Service.containerExists("storage-account", "container")).thenReturn(left(error));

        var actualRes = outputPortValidator.validate(dataProduct, outputPort, true);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(error, actualRes.getLeft());
    }

    @Test
    public void testValidateMissingStorageDependencyError() {
        var error = new FailedOperation(
                Collections.singletonList(
                        new Problem(
                                "The component urn:dmb:cmp:healthcare:vaccinations:0:outputport2 must have one dependency on a storage component")));

        var actualRes = outputPortValidator.validate(dataProduct, outputPortNoDepends, false);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(error, actualRes.getLeft());
    }

    @Test
    public void testValidateMissingStorageOnDescriptorError() {
        var error = new FailedOperation(Collections.singletonList(new Problem(
                "Output Port dependency urn:dmb:cmp:healthcare:vaccinations:0:storage not found in the descriptor")));

        var dataProduct = new DataProduct();
        dataProduct.setComponents(List.of());
        var actualRes = outputPortValidator.validate(dataProduct, outputPort, false);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(actualRes.getLeft(), error);
    }

    @Test
    public void testValidateDependencyIsNotStorageError() {
        OutputPortSpecific specific = new OutputPortSpecific();
        specific.setContainer("container");
        specific.setPath("path");
        specific.setFileFormat("CSV");

        OutputPort<OutputPortSpecific> testOp = new OutputPort<>();
        testOp.setKind("outputport");
        testOp.setId("urn:dmb:cmp:healthcare:vaccinations:0:outputport");
        testOp.setName("output port name");
        testOp.setDescription("output port desc");
        testOp.setDependsOn(List.of(outputPort.getId()));
        testOp.setSpecific(specific);

        var storageAreaNode = om.valueToTree(outputPort);
        var outputPortNode = om.valueToTree(testOp);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(storageAreaNode);
        nodes.add(outputPortNode);
        DataProduct dp = new DataProduct();
        dp.setComponents(nodes);

        var error = new FailedOperation(
                Collections.singletonList(
                        new Problem(
                                "Component 'urn:dmb:cmp:healthcare:vaccinations:0:outputport' has unexpected kind for an output port dependency. Expected: storage, found: outputport")));

        var actualRes = outputPortValidator.validate(dp, testOp, false);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(actualRes.getLeft(), error);
    }

    @Test
    public void testRetrieveStorageAccountFromInfoError() {
        var outputPortNode = om.valueToTree(outputPort);
        var storageAreaNode = om.valueToTree(storageAreaNoInfo);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(storageAreaNode);
        nodes.add(outputPortNode);
        DataProduct dp = new DataProduct();
        dp.setComponents(nodes);

        var error = new FailedOperation(Collections.singletonList(new Problem(
                "Failed retrieving deploy info from component urn:dmb:cmp:healthcare:vaccinations:0:storage")));

        var actualRes = outputPortValidator.validate(dp, outputPort, true);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(actualRes.getLeft(), error);
    }

    @Test
    public void testRetrieveStorageAccountFromUnexpectedInfoError() {
        StorageArea<Specific> storageAreaWrongInfo = new StorageArea<>();
        storageAreaWrongInfo.setId("urn:dmb:cmp:healthcare:vaccinations:0:storage");
        storageAreaWrongInfo.setName("storage name");
        storageAreaWrongInfo.setDescription("storage desc");
        storageAreaWrongInfo.setKind("storage");
        storageAreaWrongInfo.setSpecific(new Specific());

        storageAreaWrongInfo.setInfo(Optional.of(om.valueToTree(outputPort)));

        var outputPortNode = om.valueToTree(outputPort);
        var storageAreaNode = om.valueToTree(storageAreaWrongInfo);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(storageAreaNode);
        nodes.add(outputPortNode);
        DataProduct dp = new DataProduct();
        dp.setComponents(nodes);

        var expectedDesc = "Failed to deserialize the component. Details:";
        var actualRes = outputPortValidator.validate(dp, outputPort, true);

        Assertions.assertTrue(actualRes.isLeft());
        Assertions.assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            Assertions.assertTrue(p.description().startsWith(expectedDesc));
            Assertions.assertTrue(p.cause().isPresent());
        });
    }

    @Test
    public void testValidateWrongType() {
        String expectedDesc = "The component urn:dmb:cmp:healthcare:vaccinations:0:storage is not of type OutputPort";

        var actualRes = outputPortValidator.validate(dataProduct, storageArea, false);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }
}
