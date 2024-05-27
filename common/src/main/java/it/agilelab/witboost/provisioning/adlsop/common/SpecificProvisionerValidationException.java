package it.agilelab.witboost.provisioning.adlsop.common;

public class SpecificProvisionerValidationException extends RuntimeException {

    private final FailedOperation failedOperation;

    public SpecificProvisionerValidationException(FailedOperation failedOperation) {
        super();
        this.failedOperation = failedOperation;
    }

    public FailedOperation getFailedOperation() {
        return failedOperation;
    }
}
