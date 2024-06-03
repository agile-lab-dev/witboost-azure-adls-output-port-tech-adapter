package it.agilelab.witboost.provisioning.adlsop.api;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.common.SpecificProvisionerValidationException;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningRequest;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningStatus;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationError;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationResult;
import it.agilelab.witboost.provisioning.adlsop.service.provision.OutputPortHandler;
import it.agilelab.witboost.provisioning.adlsop.service.validation.ValidationService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApiServiceImplTest {

    @Mock
    private ValidationService validationService;

    @Mock
    OutputPortHandler outputPortHandler;

    @InjectMocks
    private ApiServiceImpl provisionService;

    @Test
    public void testValidateOk() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        when(validationService.validate(provisioningRequest, false))
                .thenReturn(Either.right(new ProvisionRequest<Specific>(null, null, false)));
        var expectedRes = new ValidationResult(true);

        var actualRes = provisionService.validate(provisioningRequest);

        Assertions.assertEquals(expectedRes, actualRes);
    }

    @Test
    public void testValidateError() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem("error")));
        when(validationService.validate(provisioningRequest, false)).thenReturn(Either.left(failedOperation));
        var expectedRes = new ValidationResult(false).error(new ValidationError(List.of("error")));

        var actualRes = provisionService.validate(provisioningRequest);

        Assertions.assertEquals(expectedRes, actualRes);
    }

    @Test
    public void testProvisionValidationError() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem("error")));
        when(validationService.validate(provisioningRequest, true)).thenReturn(left(failedOperation));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.provision(provisioningRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }

    @Test
    public void testProvisionUnsupportedKind() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("unsupported");
        when(validationService.validate(provisioningRequest, true))
                .thenReturn(right(new ProvisionRequest<>(null, outputPort, false)));
        String expectedDesc = "The kind 'unsupported' of the component is not supported by this Specific Provisioner";
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem(expectedDesc)));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.provision(provisioningRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }

    @Test
    public void testProvisionOutputPortOk() throws JsonProcessingException {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        OutputPortSpecific outputPortSpecific = new OutputPortSpecific();
        outputPortSpecific.setStorageAccount("storageAccount");
        outputPortSpecific.setContainer("containerName");
        outputPortSpecific.setPath("path");
        outputPortSpecific.setFileFormat("CSV");
        OutputPort<OutputPortSpecific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        var provisionRequest = new ProvisionRequest<>(null, outputPort, false);
        when(validationService.validate(provisioningRequest, true)).thenReturn(right(provisionRequest));
        AdlsGen2DirectoryInfo directoryInfo = new AdlsGen2DirectoryInfo(
                "storageAccount",
                "containerName",
                "path",
                "https://storageAccount.dfs.core.windows.net/containerName/path",
                null,
                "CSV");

        when(outputPortHandler.create(provisionRequest)).thenReturn(right(directoryInfo));

        var expectedRes = new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "")
                .info(InfoMapper.createDeployInfo(directoryInfo));
        var actualRes = provisionService.provision(provisioningRequest);

        assertEquals(
                new ObjectMapper().writeValueAsString(expectedRes), new ObjectMapper().writeValueAsString(actualRes));
    }

    @Test
    public void testProvisionOutputPortFailHandler() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        var provisionRequest = new ProvisionRequest<>(null, outputPort, false);
        when(validationService.validate(provisioningRequest, true)).thenReturn(right(provisionRequest));
        String expectedDesc = "Error on ADLS";
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem(expectedDesc)));
        when(outputPortHandler.create(provisionRequest)).thenReturn(left(failedOperation));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.provision(provisioningRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }

    @Test
    public void testUnprovisionValidationError() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem("error")));
        when(validationService.validate(provisioningRequest, true)).thenReturn(left(failedOperation));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.unprovision(provisioningRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }

    @Test
    public void testUnprovisionUnsupportedKind() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("unsupported");
        when(validationService.validate(provisioningRequest, true))
                .thenReturn(right(new ProvisionRequest<>(null, outputPort, false)));
        String expectedDesc = "The kind 'unsupported' of the component is not supported by this Specific Provisioner";
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem(expectedDesc)));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.unprovision(provisioningRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }

    @Test
    public void testUnprovisionOutputPortOk() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        var provisionRequest = new ProvisionRequest<>(null, outputPort, false);
        when(validationService.validate(provisioningRequest, true)).thenReturn(right(provisionRequest));
        when(outputPortHandler.destroy(provisionRequest)).thenReturn(right(null));
        var expectedRes = new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "");

        var actualRes = provisionService.unprovision(provisioningRequest);

        assertEquals(expectedRes, actualRes);
    }

    @Test
    public void testUnprovisionOutputPortFailHandler() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        var provisionRequest = new ProvisionRequest<>(null, outputPort, false);
        when(validationService.validate(provisioningRequest, true)).thenReturn(right(provisionRequest));
        String expectedDesc = "Error on ADLS";
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem(expectedDesc)));
        when(outputPortHandler.destroy(provisionRequest)).thenReturn(left(failedOperation));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.unprovision(provisioningRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }
}
