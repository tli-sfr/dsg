package com.ringcentral.dsg.api.rc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RcBulkAssignRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesReferencesInsideExtension() throws Exception {
        DirectoryUser user = new DirectoryUser(
                "00u1gr9kup0Np4B831d8",
                "rc20.user2@testorg.com",
                Map.of("firstName", "rc20", "lastName", "user2", "email", "rc20.user2@testorg.com"));

        String json = objectMapper.writeValueAsString(RcBulkAssignRequest.forDirectoryUser(user));
        JsonNode item = objectMapper.readTree(json).get("items").get(0);

        assertTrue(item.has("extension"));
        assertTrue(item.get("extension").has("references"));
        assertTrue(item.get("extension").has("contact"));
        assertTrue(!item.has("references") || item.get("references").isNull());
    }
}
