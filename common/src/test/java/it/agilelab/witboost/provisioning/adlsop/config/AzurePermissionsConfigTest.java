package it.agilelab.witboost.provisioning.adlsop.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(classes = AzurePermissionsConfigTest.class)
@EnableConfigurationProperties(AzurePermissionsConfig.class)
public class AzurePermissionsConfigTest {

    @Autowired
    private AzurePermissionsConfig azurePermissionsConfig;

    @TestConfiguration
    static class TestConfig {

        @Primary
        @Bean
        public AzurePermissionsConfig primaryAzurePermissionsConfig() {
            AzurePermissionsConfig config = new AzurePermissionsConfig();
            config.setClientId("testClientId");
            config.setTenantId("testTenantId");
            config.setClientSecret("testClientSecret");
            return config;
        }
    }

    @Test
    public void testConfigurationProperties() {
        assertEquals("testClientId", azurePermissionsConfig.getClientId());
        assertEquals("testTenantId", azurePermissionsConfig.getTenantId());
        assertEquals("testClientSecret", azurePermissionsConfig.getClientSecret());
    }

    @Test
    public void testToString() {
        AzurePermissionsConfig config = new AzurePermissionsConfig();
        config.setClientId("clientId");
        config.setTenantId("tenantId");
        config.setClientSecret("clientSecret");

        String expectedToString =
                "AzurePermissionsConfig(clientId=clientId, tenantId=tenantId, clientSecret=clientSecret)";
        assertEquals(expectedToString, config.toString());
    }

    @Test
    public void testCanEqual() {
        AzurePermissionsConfig config1 = new AzurePermissionsConfig();
        AzurePermissionsConfig config2 = new AzurePermissionsConfig();
        Object otherObject = new Object();

        assertTrue(config1.canEqual(config2));
        assertFalse(config1.canEqual(otherObject));
    }

    @Test
    public void testEquals() {
        AzurePermissionsConfig config1 = new AzurePermissionsConfig();
        config1.setClientId("clientId1");
        config1.setTenantId("tenantId1");
        config1.setClientSecret("clientSecret1");

        AzurePermissionsConfig config2 = new AzurePermissionsConfig();
        config2.setClientId("clientId1");
        config2.setTenantId("tenantId1");
        config2.setClientSecret("clientSecret1");

        AzurePermissionsConfig config3 = new AzurePermissionsConfig();
        config3.setClientId("clientId2");
        config3.setTenantId("tenantId2");
        config3.setClientSecret("clientSecret2");

        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertNotEquals(config2, config3);
        assertNotEquals(null, config1);
        assertNotEquals(config1, new Object());
    }

    @Test
    public void testHashCode() {
        AzurePermissionsConfig config1 = new AzurePermissionsConfig();
        config1.setClientId("clientId1");
        config1.setTenantId("tenantId1");
        config1.setClientSecret("clientSecret1");

        AzurePermissionsConfig config2 = new AzurePermissionsConfig();
        config2.setClientId("clientId1");
        config2.setTenantId("tenantId1");
        config2.setClientSecret("clientSecret1");

        AzurePermissionsConfig config3 = new AzurePermissionsConfig();
        config3.setClientId("clientId2");
        config3.setTenantId("tenantId2");
        config3.setClientSecret("clientSecret2");

        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config3.hashCode());
        assertNotEquals(config2.hashCode(), config3.hashCode());
    }
}
