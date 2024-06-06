package it.agilelab.witboost.provisioning.adlsop.bean;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureClient;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureGraphClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureClientConfig {

    @Bean
    public AzureClient azureClient(TokenCredential tokenCredential) {

        String[] scopes = new String[] {"https://graph.microsoft.com/.default"};

        GraphServiceClient graphServiceClient = new GraphServiceClient(tokenCredential, scopes);

        return new AzureGraphClient(graphServiceClient);
    }
}
