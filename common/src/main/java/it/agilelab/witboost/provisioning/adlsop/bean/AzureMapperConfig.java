package it.agilelab.witboost.provisioning.adlsop.bean;

import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureClient;
import it.agilelab.witboost.provisioning.adlsop.principalsmapping.azure.AzureMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureMapperConfig {

    @Bean
    public AzureMapper azureMapper(AzureClient azureClient) {
        return new AzureMapper(azureClient);
    }
}
