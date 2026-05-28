package com.ringcentral.dsg.worker.stub;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test-only directory adapter (enable with {@code dsg.directory.stub=true}).
 * Production sync uses {@link com.ringcentral.dsg.api.directory.OktaDirectoryPort}.
 */
public class StubDirectoryPort implements DirectoryPort {

    private static final Logger log = LoggerFactory.getLogger(StubDirectoryPort.class);

    @Override
    public List<DirectoryUser> listGroupMembers(String accountId, String directoryGroupId) {
        log.warn(
                "[DSG stub] dsg.directory.stub=true — not calling Okta. Set dsg.directory.stub=false to sync real users.");
        String group = directoryGroupId != null ? directoryGroupId : "default-group";
        return List.of(
                testUser(accountId, "user-1", "Sales", group),
                testUser(accountId, "user-2", "Engineering", group));
    }

    /** Attributes only — no fabricated names/emails; IT supplies profile.* when exercising mapping. */
    private static DirectoryUser testUser(
            String accountId, String suffix, String department, String group) {
        return new DirectoryUser(
                accountId + "-" + suffix,
                null,
                Map.of("department", department, "profile.department", department, "group", group));
    }
}
