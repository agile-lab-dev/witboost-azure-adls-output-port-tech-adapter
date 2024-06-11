package it.agilelab.witboost.provisioning.adlsop.model.azure;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class AdlsGen2DirectoryInfo {
    private String storageAccount;
    private String containerName;
    private String path;
    private String adlsURI;
    private String storageExplorerURI;
    private String fileFormat;
}
