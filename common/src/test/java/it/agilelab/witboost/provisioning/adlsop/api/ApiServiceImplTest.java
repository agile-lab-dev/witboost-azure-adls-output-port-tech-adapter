package it.agilelab.witboost.provisioning.adlsop.api;

import static org.mockito.Mockito.when;

import io.vavr.control.Either;
import it.agilelab.witboost.provisioning.adlsop.common.FailedOperation;
import it.agilelab.witboost.provisioning.adlsop.common.Problem;
import it.agilelab.witboost.provisioning.adlsop.common.SpecificProvisionerValidationException;
import it.agilelab.witboost.provisioning.adlsop.model.ProvisionRequest;
import it.agilelab.witboost.provisioning.adlsop.model.Specific;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.DescriptorKind;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ProvisioningRequest;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationError;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.ValidationResult;
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
    void testProvisionKO() {

        ProvisioningRequest provisioningRequest =
                new ProvisioningRequest(DescriptorKind.COMPONENT_DESCRIPTOR, "", false);
        var failedOperation = new FailedOperation(
                Collections.singletonList(new Problem("Implement the provision logic based on your requirements!")));

        var exception = Assertions.assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.provision(provisioningRequest));

        Assertions.assertEquals(failedOperation, exception.getFailedOperation());
    }

    @Test
    void testUnprovisionKO() {

        ProvisioningRequest provisioningRequest =
                new ProvisioningRequest(DescriptorKind.COMPONENT_DESCRIPTOR, "", false);
        var failedOperation = new FailedOperation(
                Collections.singletonList(new Problem("Implement the unprovision logic based on your requirements!")));

        var exception = Assertions.assertThrows(
                SpecificProvisionerValidationException.class, () -> provisionService.unprovision(provisioningRequest));

        Assertions.assertEquals(failedOperation, exception.getFailedOperation());
    }
}
