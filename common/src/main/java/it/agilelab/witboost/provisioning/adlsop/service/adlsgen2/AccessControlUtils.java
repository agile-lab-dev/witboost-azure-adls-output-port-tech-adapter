package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import com.azure.storage.file.datalake.models.AccessControlType;
import com.azure.storage.file.datalake.models.PathAccessControlEntry;
import com.azure.storage.file.datalake.models.RolePermissions;
import io.vavr.control.Option;
import java.util.List;

public class AccessControlUtils {

    public static PathAccessControlEntry buildPathAccessControlEntry(
            RolePermissions rolePermissions,
            AccessControlType accessControlType,
            boolean isDefaultScope,
            Option<String> entityId) {
        PathAccessControlEntry ret = new PathAccessControlEntry()
                .setDefaultScope(isDefaultScope)
                .setAccessControlType(accessControlType)
                .setPermissions(rolePermissions);
        entityId.forEach(ret::setEntityId);

        return ret;
    }

    public static List<PathAccessControlEntry> getDefaultAccessControlEntries() {

        RolePermissions ownerPermission = new RolePermissions();
        ownerPermission.setReadPermission(true).setWritePermission(true).setExecutePermission(true);

        RolePermissions groupPermission = new RolePermissions();
        groupPermission.setReadPermission(true).setWritePermission(false).setExecutePermission(true);

        RolePermissions otherPermission = new RolePermissions();
        otherPermission.setReadPermission(true);

        // Create owner entry.
        PathAccessControlEntry ownerEntry = AccessControlUtils.buildPathAccessControlEntry(
                ownerPermission, AccessControlType.USER, false, Option.none());
        // Create group entry.
        PathAccessControlEntry groupEntry = AccessControlUtils.buildPathAccessControlEntry(
                groupPermission, AccessControlType.GROUP, false, Option.none());
        // Create other entry.
        PathAccessControlEntry otherEntry = AccessControlUtils.buildPathAccessControlEntry(
                otherPermission, AccessControlType.OTHER, false, Option.none());

        return List.of(ownerEntry, groupEntry, otherEntry);
    }
}
