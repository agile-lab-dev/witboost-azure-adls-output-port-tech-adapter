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
@Valid
@Getter
@EqualsAndHashCode
@ToString
public class StoragePrivateInfo {

    @JsonProperty("storage_account_name")
    @NotNull
    private StringInfoObject storageAccountNameInfo;

    @JsonCreator
    public StoragePrivateInfo(
            @JsonProperty(value = "storage_account_name", required = true) StringInfoObject storageAccountNameInfo) {
        this.storageAccountNameInfo = storageAccountNameInfo;
    }
}
