package it.agilelab.witboost.provisioning.adlsop.bean;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import it.agilelab.witboost.provisioning.adlsop.config.AzurePermissionsConfig;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureClient;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureGraphClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AzurePermissionsConfig.class)
public class AzureClientConfig {

    @Bean
    public AzureClient azureClient(AzurePermissionsConfig azurePermissionsConfig) {

        String clientId = azurePermissionsConfig.getClientId();
        String tenantId = azurePermissionsConfig.getTenantId();
        String clientSecret = azurePermissionsConfig.getClientSecret();

        String[] scopes = new String[] {"https://graph.microsoft.com/.default"};

        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .clientSecret(clientSecret)
                .build();

        GraphServiceClient graphServiceClient = new GraphServiceClient(credential, scopes);

        return new AzureGraphClient(graphServiceClient);
    }
}
