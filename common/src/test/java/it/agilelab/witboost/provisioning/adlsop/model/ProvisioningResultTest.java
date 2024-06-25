package it.agilelab.witboost.provisioning.adlsop.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProvisioningResultTest {

    @Test
    void getStorageAccountNameReturnsOk() {
        var storageAccountName = "storageAccount";
        ProvisioningResult info = new ProvisioningResult(new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject(storageAccountName)))));

        var actualRes = info.getInfo().getStorageAccountName();
        assertTrue(actualRes.isRight());
        assertEquals(storageAccountName, actualRes.get());
    }

    @Test
    void getStorageAccountNameReturnsError() {
        String storageAccountName = null;
        ProvisioningResult info = new ProvisioningResult(new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject(storageAccountName)))));

        var actualRes = info.getInfo().getStorageAccountName();
        var expectedError = "Failed retrieving Storage Account name from deploy private info";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testEquals() {
        ProvisioningResult info = new ProvisioningResult(new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount")))));
        ProvisioningResult info2 = new ProvisioningResult(new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount")))));
        assertEquals(info, info2);
    }

    @Test
    void testHashCode() {
        ProvisioningResult info = new ProvisioningResult(new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount")))));
        ProvisioningResult info2 = new ProvisioningResult(new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount")))));
        assertEquals(info.hashCode(), info2.hashCode());
    }
}
