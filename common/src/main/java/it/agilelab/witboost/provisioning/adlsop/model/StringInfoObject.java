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
@EqualsAndHashCode
@ToString
@Getter
public class StringInfoObject {

    @NotNull
    private String value;

    @JsonCreator
    public StringInfoObject(@JsonProperty(value = "value", required = true) String value) {
        this.value = value;
    }
}
