package it.agilelab.witboost.provisioning.adlsop.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.agilelab.witboost.provisioning.adlsop.model.Descriptor;
import it.agilelab.witboost.provisioning.adlsop.model.Specific;
import it.agilelab.witboost.provisioning.adlsop.model.StorageDeployInfo;
import it.agilelab.witboost.provisioning.adlsop.util.ResourceUtils;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParserTest {

    @Test
    void testParseStorageDescriptorOk() throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_storage.yml");

        var actualResult = Parser.parseDescriptor(ymlDescriptor);

        assertTrue(actualResult.isRight());
    }

    @Test
    void testParseOutputPortDescriptorOk() throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_outputport.yml");

        var actualResult = Parser.parseDescriptor(ymlDescriptor);

        assertTrue(actualResult.isRight());
    }

    @Test
    void testParseWorkloadDescriptorOk() throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_workload.yml");

        var actualResult = Parser.parseDescriptor(ymlDescriptor);

        assertTrue(actualResult.isRight());
    }

    @Test
    public void testParseStorageDescriptorFail() {
        String invalidDescriptor = "an_invalid_descriptor";
        String expectedDesc = "Failed to deserialize the Yaml Descriptor. Details: ";

        var actualRes = Parser.parseDescriptor(invalidDescriptor);

        assertTrue(actualRes.isLeft());
        Assertions.assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertTrue(p.description().startsWith(expectedDesc));
            assertTrue(p.cause().isPresent());
        });
    }

    @Test
    public void testParseOutputPortComponentOk() throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_outputport.yml");
        var eitherDescriptor = Parser.parseDescriptor(ymlDescriptor);
        assertTrue(eitherDescriptor.isRight());
        Descriptor descriptor = eitherDescriptor.get();
        String componentIdToProvision = "urn:dmb:cmp:healthcare:vaccinations:0:hdfs-output-port";
        var optionalComponent = descriptor.getDataProduct().getComponentToProvision(componentIdToProvision);
        assertTrue(optionalComponent.isDefined());
        JsonNode component = optionalComponent.get();

        var actualRes = Parser.parseComponent(component, Specific.class);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testParseStorageComponentFail() {
        JsonNode node = null;
        String expectedDesc = "Failed to deserialize the component. Details: ";

        var actualRes = Parser.parseComponent(node, Specific.class);

        assertTrue(actualRes.isLeft());
        Assertions.assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertTrue(p.description().startsWith(expectedDesc));
            assertTrue(p.cause().isPresent());
        });
    }

    @Test
    public void testParseObject() {
        ObjectMapper om = new ObjectMapper();

        var t = new StorageDeployInfo((String) null);

        var r = Parser.parseObject(om.valueToTree(t), StorageDeployInfo.class);
        assertTrue(r.isRight());
        assertEquals(t, r.get());
    }

    @Test
    public void testParseObjectError() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        var string = "{\"aWrongField\": \"aWrongValue\"}";

        var r = Parser.parseObject(om.readTree(string), StorageDeployInfo.class);
        assertTrue(r.isLeft());
    }

    @Test
    public void testParseStringObject() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        var t = new StorageDeployInfo((String) null);

        var r = Parser.parseObject(om.writeValueAsString(t), StorageDeployInfo.class);
        assertTrue(r.isRight());
        assertEquals(t, r.get());
    }

    @Test
    public void testParseStringObjectError() {

        String string = "{\"aWrongField\": \"aWrongValue\"}";

        var r = Parser.parseObject(string, StorageDeployInfo.class);
        assertTrue(r.isLeft());
    }

    @Test
    public void testParseStringObjectMalformedJson() {

        String string = "aWrongField: aWrongValue";

        var r = Parser.parseObject(string, StorageDeployInfo.class);
        assertTrue(r.isLeft());
    }
}
