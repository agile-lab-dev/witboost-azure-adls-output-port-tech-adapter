package it.agilelab.witboost.provisioning.adlsop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Valid
public class OutputPortSpecific extends Specific {

    @NotBlank
    String storageAccount;

    @NotBlank
    String container;

    @NotBlank
    String path;
}
