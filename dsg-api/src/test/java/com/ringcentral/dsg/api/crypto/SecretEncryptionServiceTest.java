package com.ringcentral.dsg.api.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SecretEncryptionServiceTest {

    @Test
    void encryptsAndDecryptsRoundTrip() {
        SecretEncryptionService service = new SecretEncryptionService("unit-test-secret-key");
        String encrypted = service.encrypt("my-client-secret");
        assertNotEquals("my-client-secret", encrypted);
        assertEquals("my-client-secret", service.decrypt(encrypted));
    }
}
