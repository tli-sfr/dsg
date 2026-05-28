package com.ringcentral.dsg.mapping;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeMappingApplierTest {

    @Test
    void mapsOktaProfileFieldsToRcAttributes() {
        DirectoryUser user = new DirectoryUser(
                "okta-1",
                "wrong@example.com",
                Map.of(
                        "profile.firstName", "Pat",
                        "profile.lastName", "Lee",
                        "profile.email", "pat.lee@company.test",
                        "department", "Sales"));

        DirectoryUser mapped = AttributeMappingApplier.apply(
                user,
                List.of(
                        new AttributeMapping("profile.firstName", "firstName"),
                        new AttributeMapping("profile.lastName", "lastName"),
                        new AttributeMapping("profile.email", "email"),
                        new AttributeMapping("department", "department")));

        assertEquals("pat.lee@company.test", mapped.email());
        assertEquals("Pat", mapped.attributes().get("firstName"));
        assertEquals("Lee", mapped.attributes().get("lastName"));
        assertEquals("Sales", mapped.attributes().get("department"));
    }
}
