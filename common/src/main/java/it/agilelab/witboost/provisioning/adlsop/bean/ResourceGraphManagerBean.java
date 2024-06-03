package it.agilelab.witboost.provisioning.adlsop.bean;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resourcegraph.ResourceGraphManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceGraphManagerBean {

    @Bean
    public ResourceGraphManager resourceGraphManager(TokenCredential tokenCredential) {
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        return ResourceGraphManager.authenticate(tokenCredential, profile);
    }
}
