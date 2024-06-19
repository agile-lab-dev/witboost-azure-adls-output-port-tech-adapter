# ADLS Gen2 Output Port Specific Provisioner Configuration

Application configuration is handled using the features provided by Spring Boot. You can find the default settings in the `application.yml`. Customize it and use the `spring.config.location` system property or the other options provided by the framework according to your needs.

### Azure configuration

The Specific Provisioner allows for two different modes of authentication against an Azure environment to be used concurrently in order to access ADLS Gen2 and the Microsoft Graph using one set of credentials each.

To access ADLS Gen2 the `DefaulAzureCredential` is used, so whichever method of authentication is configured on the execution environment will be used.

The Microsoft Graph expects a set of service principals credentials stored in the Spring Boot configuration. As seen in the table below, these are set by default to the default Azure environment variables, but they can be overridden if necessary.

| Configuration                    | Description                                               | Default                  |
|:---------------------------------|:----------------------------------------------------------|:-------------------------|
| `azure.permissions.clientId`     | Service Principal Client ID to access Microsoft Graph     | `${AZURE_CLIENT_ID}`     |
| `azure.permissions.tenantId`     | Azure Tenant ID                                           | `${AZURE_TENANT_ID}`     |
| `azure.permissions.clientSecret` | Service Principal Client Secret to access Microsoft Graph | `${AZURE_CLIENT_SECRET}` |
