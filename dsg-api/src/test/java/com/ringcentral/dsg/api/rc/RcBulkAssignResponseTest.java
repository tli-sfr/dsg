package com.ringcentral.dsg.api.rc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RcBulkAssignResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesExtensionIdFromItems() throws Exception {
        String json =
                """
                {
                  "items": [
                    {
                      "successful": true,
                      "extension": {
                        "id": 1234567,
                        "uri": "/restapi/v1.0/account/664287016/extension/1234567",
                        "type": "User",
                        "status": "Enabled",
                        "contact": {
                          "firstName": "John",
                          "lastName": "Doe",
                          "email": "john.doe@mycompany.com"
                        },
                        "references": [
                          { "type": "CustomerDirectoryId", "ref": "ABCDEF" }
                        ]
                      }
                    }
                  ]
                }
                """;

        RcBulkAssignResponse response = objectMapper.readValue(json, RcBulkAssignResponse.class);

        assertTrue(response.isProvisionSuccess());
        assertEquals("1234567", response.firstExtensionId());
    }

    @Test
    void parsesExtensionIdFromUrlWhenSuccessfulOmitted() throws Exception {
        String json =
                """
                {
                  "items": [
                    {
                      "extension": {
                        "id": "7654321",
                        "url": "/restapi/v1.0/account/664287016/extension/7654321"
                      }
                    }
                  ]
                }
                """;

        RcBulkAssignResponse response = objectMapper.readValue(json, RcBulkAssignResponse.class);

        assertTrue(response.isProvisionSuccess());
        assertEquals("7654321", response.firstExtensionId());
    }

    @Test
    void parsesExtensionIdFromUrlWhenIdMissing() throws Exception {
        String json =
                """
                {
                  "items": [
                    {
                      "successful": true,
                      "extension": {
                        "url": "/restapi/v1.0/account/664287016/extension/9988776"
                      }
                    }
                  ]
                }
                """;

        RcBulkAssignResponse response = objectMapper.readValue(json, RcBulkAssignResponse.class);

        assertTrue(response.isProvisionSuccess());
        assertEquals("9988776", response.firstExtensionId());
    }
}
