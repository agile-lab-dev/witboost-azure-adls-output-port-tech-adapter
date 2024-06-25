package it.agilelab.witboost.provisioning.adlsop.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StorageDeployInfoTest {

    @Test
    void getStorageAccountNameReturnsOk() {
        var storageAccountName = "storageAccount";
        StorageDeployInfo info = new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject(storageAccountName))));

        var actualRes = info.getStorageAccountName();
        assertTrue(actualRes.isRight());
        assertEquals(storageAccountName, actualRes.get());
    }

    @Test
    void getStorageAccountNameReturnsError() {
        String storageAccountName = null;
        StorageDeployInfo info = new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject(storageAccountName))));

        var actualRes = info.getStorageAccountName();
        var expectedError = "Failed retrieving Storage Account name from deploy private info";
        assertTrue(actualRes.isLeft());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedError, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    void testEquals() {
        StorageDeployInfo info = new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount"))));
        StorageDeployInfo info2 = new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount"))));
        assertEquals(info, info2);
    }

    @Test
    void testHashCode() {
        StorageDeployInfo info = new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount"))));
        StorageDeployInfo info2 = new StorageDeployInfo(
                new StoragePrivateOutputInfo(new StoragePrivateInfo(new StringInfoObject("storageAccount"))));
        assertEquals(info.hashCode(), info2.hashCode());
    }
}
