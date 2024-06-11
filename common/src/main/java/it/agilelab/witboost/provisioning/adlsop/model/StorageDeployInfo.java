package it.agilelab.witboost.provisioning.adlsop.model;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@EqualsAndHashCode
@ToString
@Valid
public class StorageDeployInfo {

    @NotNull
    private StoragePrivateInfo privateInfo;

    public StorageDeployInfo(String storageAccountName) {
        privateInfo = new StoragePrivateInfo(new StringInfoObject(storageAccountName));
    }

    @JsonCreator
    public StorageDeployInfo(@JsonProperty(value = "privateInfo", required = true) StoragePrivateInfo privateInfo) {
        this.privateInfo = privateInfo;
    }

    @JsonIgnore
    public Either<FailedOperation, String> getStorageAccountName() {
        String storageAccountName = privateInfo.getStorageAccountNameInfo().getValue();
        if (storageAccountName != null) {
            return right(storageAccountName);
        }
        return left(new FailedOperation(
                Collections.singletonList(new Problem("Failed retrieving Storage Account name from component"))));
    }
}
