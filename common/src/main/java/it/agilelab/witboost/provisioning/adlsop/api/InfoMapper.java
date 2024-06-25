package it.agilelab.witboost.provisioning.adlsop.api;

import it.agilelab.witboost.provisioning.adlsop.model.PublicInfoObject;
import it.agilelab.witboost.provisioning.adlsop.model.StoragePrivateOutputInfo;
import it.agilelab.witboost.provisioning.adlsop.model.azure.AdlsGen2DirectoryInfo;
import it.agilelab.witboost.provisioning.adlsop.openapi.model.Info;
import java.util.Map;

public class InfoMapper {
    public static Info createDeployInfo(AdlsGen2DirectoryInfo directoryInfo) {
        return new Info(
                Map.of(
                        "storageAccount",
                                PublicInfoObject.createLinkInfo(
                                        "Storage Account",
                                        directoryInfo.getStorageAccount(),
                                        directoryInfo.getStorageExplorerURI()),
                        "container",
                                PublicInfoObject.createTextInfo("Container Name", directoryInfo.getContainerName()),
                        "path", PublicInfoObject.createTextInfo("Folder path", directoryInfo.getPath()),
                        "adlsUri", PublicInfoObject.createTextInfo("ADLS URI", directoryInfo.getAdlsURI()),
                        "format", PublicInfoObject.createTextInfo("File format", directoryInfo.getFileFormat())),
                new StoragePrivateOutputInfo(directoryInfo.getStorageAccount()));
    }
}
