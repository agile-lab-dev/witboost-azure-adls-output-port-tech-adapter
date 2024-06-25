package it.agilelab.witboost.provisioning.adlsop.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
@ToString
@Valid
public class ProvisioningResult {

    @NotNull
    @JsonProperty("info")
    StorageDeployInfo info;

    public ProvisioningResult(String storageAccountName) {
        info = new StorageDeployInfo(storageAccountName);
    }

    @JsonCreator
    public ProvisioningResult(@JsonProperty(value = "info", required = true) StorageDeployInfo info) {
        this.info = info;
    }
}
