package it.agilelab.witboost.provisioning.adlsop.model.azure;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AdlsGen2DirectoryInfoTest {

    @Test
    void testEquals() {
        AdlsGen2DirectoryInfo info = new AdlsGen2DirectoryInfo(
                "storageAccount", "containerName", "path", "adlsURI", "storageExplorerURI", "fileFormat");
        AdlsGen2DirectoryInfo info2 = new AdlsGen2DirectoryInfo(
                "storageAccount", "containerName", "path", "adlsURI", "storageExplorerURI", "fileFormat");
        assertEquals(info, info2);
    }

    @Test
    void testHashCode() {
        AdlsGen2DirectoryInfo info = new AdlsGen2DirectoryInfo(
                "storageAccount", "containerName", "path", "adlsURI", "storageExplorerURI", "fileFormat");
        AdlsGen2DirectoryInfo info2 = new AdlsGen2DirectoryInfo(
                "storageAccount", "containerName", "path", "adlsURI", "storageExplorerURI", "fileFormat");
        assertEquals(info.hashCode(), info2.hashCode());
    }
}
