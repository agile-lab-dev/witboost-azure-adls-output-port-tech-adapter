package it.agilelab.witboost.provisioning.adlsop.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StorageAccountInfoTest {

    @Test
    void testEquals() {
        StorageAccountInfo info = new StorageAccountInfo(
                "subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account",
                "name",
                "resourceGroup",
                "subscriptionId",
                "tenantId",
                "type");
        StorageAccountInfo info2 = new StorageAccountInfo(
                "subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account",
                "name",
                "resourceGroup",
                "subscriptionId",
                "tenantId",
                "type");
        assertEquals(info, info2);
    }

    @Test
    void testHashCode() {
        StorageAccountInfo info = new StorageAccountInfo(
                "subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account",
                "name",
                "resourceGroup",
                "subscriptionId",
                "tenantId",
                "type");
        StorageAccountInfo info2 = new StorageAccountInfo(
                "subscriptions/1234-5678-90ab-cdef/resourceGroups/resource-group/providers/Microsoft.Storage/storageAccounts/storage-account",
                "name",
                "resourceGroup",
                "subscriptionId",
                "tenantId",
                "type");
        assertEquals(info.hashCode(), info2.hashCode());
    }
}
