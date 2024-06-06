package it.agilelab.witboost.provisioning.adlsop.service.adlsgen2;

import static org.junit.jupiter.api.Assertions.*;

import com.azure.storage.file.datalake.models.AccessControlType;
import com.azure.storage.file.datalake.models.PathAccessControlEntry;
import com.azure.storage.file.datalake.models.RolePermissions;
import io.vavr.control.Option;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccessControlUtilsTest {

    @Test
    void buildPathAccessControlEntryDefaultRole() {
        var rolePermissions = new RolePermissions().setReadPermission(true);
        PathAccessControlEntry expected = new PathAccessControlEntry()
                .setAccessControlType(AccessControlType.USER)
                .setDefaultScope(false)
                .setPermissions(rolePermissions);

        var actualRes = AccessControlUtils.buildPathAccessControlEntry(
                rolePermissions, AccessControlType.USER, false, Option.none());
        assertEquals(expected, actualRes);
    }

    @Test
    void buildPathAccessControlEntryWithObjectId() {
        var rolePermissions = new RolePermissions().setReadPermission(true);
        PathAccessControlEntry expected = new PathAccessControlEntry()
                .setAccessControlType(AccessControlType.USER)
                .setDefaultScope(false)
                .setPermissions(rolePermissions)
                .setEntityId("1234-5678-90ab-cdef");

        var actualRes = AccessControlUtils.buildPathAccessControlEntry(
                rolePermissions, AccessControlType.USER, false, Option.of("1234-5678-90ab-cdef"));
        assertEquals(expected, actualRes);
    }

    @Test
    void getDefaultAccessControlEntries() {
        var expected = List.of(
                AccessControlUtils.buildPathAccessControlEntry(
                        RolePermissions.parseSymbolic("rwx", false), AccessControlType.USER, false, Option.none()),
                AccessControlUtils.buildPathAccessControlEntry(
                        RolePermissions.parseSymbolic("r-x", false), AccessControlType.GROUP, false, Option.none()),
                AccessControlUtils.buildPathAccessControlEntry(
                        RolePermissions.parseSymbolic("r--", false), AccessControlType.OTHER, false, Option.none()));

        var actualRes = AccessControlUtils.getDefaultAccessControlEntries();
        assertEquals(expected, actualRes);
    }
}
