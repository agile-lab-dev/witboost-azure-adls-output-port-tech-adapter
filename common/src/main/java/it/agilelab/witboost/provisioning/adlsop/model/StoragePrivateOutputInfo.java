package it.agilelab.witboost.provisioning.adlsop.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@ToString
@Valid
public class StoragePrivateOutputInfo {

    @NotNull
    @JsonProperty("outputs")
    private StoragePrivateInfo outputs;

    @JsonCreator
    public StoragePrivateOutputInfo(@JsonProperty(value = "outputs", required = true) StoragePrivateInfo outputs) {
        this.outputs = outputs;
    }
}
