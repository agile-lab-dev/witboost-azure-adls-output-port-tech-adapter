package it.agilelab.witboost.provisioning.adlsop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "azure.permissions")
public class AzurePermissionsConfig {
    private String clientId;
    private String tenantId;
    private String clientSecret;
}
