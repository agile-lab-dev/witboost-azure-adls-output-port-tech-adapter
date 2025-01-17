package it.agilelab.witboost.provisioning.adlsop.api;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;
import static it.agilelab.witboost.provisioning.adlsop.common.TestFixtures.buildConstraintViolation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.common.SpecificProvisionerValidationException;
import it.agilelab.witboost.provisioning.adlsop.model.*;
import it.agilelab.witboost.provisioning.adlsop.model.azure.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.*;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisionInfo;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningRequest;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningStatus;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.UpdateAclRequest;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationError;
import it.agilelab.witboost.provisioning.adlsop.service.provision.OutputPortHandler;
import it.agilelab.witboost.provisioning.adlsop.service.validation.ValidationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    public void testValidateThrowsConstraintException() {
        ProvisioningRequest provisioningRequest = new ProvisioningRequest();
        Set<ConstraintViolation<?>> violations = Set.of(buildConstraintViolation("is not valid", "path.to.field"));
        ConstraintViolationException error = new ConstraintViolationException(violations);

        when(validationService.validate(provisioningRequest, false)).thenThrow(error);
        var expectedRes = new ValidationResult(false).error(new ValidationError(List.of("path.to.field is not valid")));

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

    @Test
    public void testUpdateAclValidationError() {
        UpdateAclRequest updateAclRequest = new UpdateAclRequest(List.of(), new ProvisionInfo("request", ""));
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem("error")));
        when(validationService.validate(any(ProvisioningRequest.class), eq(false)))
                .thenReturn(left(failedOperation));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.updateAcl(updateAclRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }

    @Test
    public void testUpdateAclUnsupportedKind() {
        UpdateAclRequest updateAclRequest = new UpdateAclRequest(List.of(), new ProvisionInfo("request", ""));
        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("unsupported");
        when(validationService.validate(any(ProvisioningRequest.class), eq(false)))
                .thenReturn(right(new ProvisionRequest<>(null, outputPort, false)));
        String expectedDesc = "The kind 'unsupported' of the component is not supported by this Specific Provisioner";
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem(expectedDesc)));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.updateAcl(updateAclRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }

    @Test
    public void testUpdateAclOutputPortOk() {
        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        UpdateAclRequest updateAclRequest = new UpdateAclRequest(
                users,
                new ProvisionInfo(
                        "request",
                        "{\"info\":{\"privateInfo\":{\"outputs\":{\"storage_account_name\":{\"value\":\"storageAccountName\"}}},\"publicInfo\":{}}}"));
        var storageInfo = new ProvisioningResult("storageAccountName");

        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        var expectedRes = new ProvisioningStatus(ProvisioningStatus.StatusEnum.COMPLETED, "");
        var provisionRequest = new ProvisionRequest<>(null, outputPort, false);
        when(validationService.validate(any(ProvisioningRequest.class), eq(false)))
                .thenReturn(right(provisionRequest));
        when(outputPortHandler.updateAcl(users, provisionRequest, storageInfo)).thenReturn(right(expectedRes));

        var actualRes = provisionService.updateAcl(updateAclRequest);

        assertEquals(expectedRes, actualRes);
    }

    @Test
    public void testUpdateAclOutputPortInfoParsingError() {
        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        UpdateAclRequest updateAclRequest =
                new UpdateAclRequest(users, new ProvisionInfo("request", "{wrongJSONInfo}"));

        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        var provisionRequest = new ProvisionRequest<>(null, outputPort, false);
        when(validationService.validate(any(ProvisioningRequest.class), eq(false)))
                .thenReturn(right(provisionRequest));

        assertThrows(SpecificProvisionerValidationException.class, () -> provisionService.updateAcl(updateAclRequest));
    }

    @Test
    public void testUpdateAclOutputPortFailHandler() {
        var users = List.of("user:john.doe_agilelab.it", "user:alice_agilelab.it");
        UpdateAclRequest updateAclRequest = new UpdateAclRequest(
                users,
                new ProvisionInfo(
                        "request",
                        "{\"info\":{\"privateInfo\":{\"outputs\":{\"storage_account_name\":{\"value\":\"storageAccountName\"}}},\"publicInfo\":{}}}"));
        var storageInfo = new ProvisioningResult("storageAccountName");

        OutputPort<Specific> outputPort = new OutputPort<>();
        outputPort.setKind("outputport");
        var provisionRequest = new ProvisionRequest<>(null, outputPort, false);
        when(validationService.validate(any(ProvisioningRequest.class), eq(false)))
                .thenReturn(right(provisionRequest));
        String expectedDesc = "Error on ADLS";
        var failedOperation = new FailedOperation(Collections.singletonList(new Problem(expectedDesc)));
        when(outputPortHandler.updateAcl(users, provisionRequest, storageInfo)).thenReturn(left(failedOperation));

        var ex = assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.updateAcl(updateAclRequest));
        assertEquals(failedOperation, ex.getFailedOperation());
    }
}
