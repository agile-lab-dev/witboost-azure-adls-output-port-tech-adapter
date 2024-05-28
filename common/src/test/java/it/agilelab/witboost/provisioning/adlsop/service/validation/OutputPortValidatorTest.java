package it.agilelab.witboost.provisioning.adlsop.service.validation;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.service.adlsgen2.AdlsGen2ServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @Test
    public void testValidateNoStorageAccountOk() {

        OutputPortSpecific specific = new OutputPortSpecific();
        specific.setStorageAccount("storage-account");
        specific.setContainer("container");
        specific.setPath("path/to/folder");

        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setId("my_id_outputport");
        outputPort.setName("outputport name");
        outputPort.setDescription("outputport desc");
        outputPort.setKind("outputport");
        outputPort.setSpecific(specific);

        DataProduct dataProduct = new DataProduct();
        dataProduct.setId("my_dp");
        ObjectMapper om = new ObjectMapper();
        var outputPortNode = om.valueToTree(outputPort);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(outputPortNode);
        dataProduct.setComponents(nodes);

        var actualRes = outputPortValidator.validate(dataProduct, outputPort, false);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testValidateWithStorageAccountOk() {

        OutputPortSpecific specific = new OutputPortSpecific();
        specific.setStorageAccount("storage-account");
        specific.setContainer("container");
        specific.setPath("path/to/folder");

        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setId("my_id_outputport");
        outputPort.setName("outputport name");
        outputPort.setDescription("outputport desc");
        outputPort.setKind("outputport");
        outputPort.setSpecific(specific);

        DataProduct dataProduct = new DataProduct();
        dataProduct.setId("my_dp");
        ObjectMapper om = new ObjectMapper();
        var outputPortNode = om.valueToTree(outputPort);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(outputPortNode);
        dataProduct.setComponents(nodes);

        when(adlsGen2Service.containerExists("storage-account", "container")).thenReturn(right(true));

        var actualRes = outputPortValidator.validate(dataProduct, outputPort, true);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testValidateWithStorageAccountNotFound() {

        OutputPortSpecific specific = new OutputPortSpecific();
        specific.setStorageAccount("storage-account");
        specific.setContainer("container");
        specific.setPath("path/to/folder");

        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setId("my_id_outputport");
        outputPort.setName("outputport name");
        outputPort.setDescription("outputport desc");
        outputPort.setKind("outputport");
        outputPort.setSpecific(specific);

        DataProduct dataProduct = new DataProduct();
        dataProduct.setId("my_dp");
        ObjectMapper om = new ObjectMapper();
        var outputPortNode = om.valueToTree(outputPort);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(outputPortNode);
        dataProduct.setComponents(nodes);

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

        OutputPortSpecific specific = new OutputPortSpecific();
        specific.setStorageAccount("storage-account");
        specific.setContainer("container");
        specific.setPath("path/to/folder");

        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setId("my_id_outputport");
        outputPort.setName("outputport name");
        outputPort.setDescription("outputport desc");
        outputPort.setKind("outputport");
        outputPort.setSpecific(specific);

        DataProduct dataProduct = new DataProduct();
        dataProduct.setId("my_dp");
        ObjectMapper om = new ObjectMapper();
        var outputPortNode = om.valueToTree(outputPort);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(outputPortNode);
        dataProduct.setComponents(nodes);

        var error = new FailedOperation(Collections.singletonList(new Problem("Error")));

        when(adlsGen2Service.containerExists("storage-account", "container")).thenReturn(left(error));

        var actualRes = outputPortValidator.validate(dataProduct, outputPort, true);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(actualRes.getLeft(), error);
    }

    @Test
    public void testValidateWrongType() {
        StorageArea<Specific> storageArea = new StorageArea<>();
        storageArea.setId("my_id_storage");
        storageArea.setName("name");
        storageArea.setKind("storage");
        storageArea.setDescription("description");
        storageArea.setSpecific(new Specific());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setId("my_dp");
        ObjectMapper om = new ObjectMapper();
        var storageAreaNode = om.valueToTree(storageArea);
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(storageAreaNode);
        dataProduct.setComponents(nodes);

        String expectedDesc = "The component my_id_storage is not of type OutputPort";

        var actualRes = outputPortValidator.validate(dataProduct, storageArea, false);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }
}
