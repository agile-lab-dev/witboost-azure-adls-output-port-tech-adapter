package it.agilelab.witboost.provisioning.adlsop.model.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class StorageAccountInfo {
    private String id;
    private String name;
    private String resourceGroup;
    private String subscriptionId;
    private String tenantId;
    private String type;
}
