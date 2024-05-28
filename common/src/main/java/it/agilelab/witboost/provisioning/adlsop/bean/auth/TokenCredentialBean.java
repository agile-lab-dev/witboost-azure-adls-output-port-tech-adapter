package it.agilelab.witboost.provisioning.adlsop.bean.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenCredentialBean {

    @Bean
    TokenCredential tokenCredential() {
        return new DefaultAzureCredentialBuilder().build();
    }
}
