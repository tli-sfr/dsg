package com.ringcentral.dsg.worker.rules;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.persistence.model.RuleBasedAttributeMappingRecord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBasedMappingApplierTest {

    @Test
    void addsRcAttributeWhenDirectoryValueMatches() {
        DirectoryUser user = new DirectoryUser("u1", "u1@example.com", Map.of("department", "Sales"));
        DirectoryUser enriched = RuleBasedMappingApplier.apply(
                user,
                List.of(new RuleBasedAttributeMappingRecord("user.department", "Sales", "ROLE", "SalesManager")));

        assertEquals("SalesManager", enriched.attributes().get("rc.ROLE"));
    }
}
