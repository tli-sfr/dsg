package com.ringcentral.dsg.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DirectorySyncHashCalculatorTest {

    private static final List<AttributeMapping> MAPPINGS = List.of(
            new AttributeMapping("profile.firstName", "firstName"),
            new AttributeMapping("profile.email", "email"));

    @Test
    void sameMappedValuesProduceSameHash() {
        DirectoryUser user1 = new DirectoryUser(
                "ext-1", "a@test.com", Map.of("firstName", "Alex", "email", "a@test.com"));
        DirectoryUser user2 = new DirectoryUser(
                "ext-2", "a@test.com", Map.of("firstName", "Alex", "email", "a@test.com"));
        assertEquals(
                DirectorySyncHashCalculator.compute(user1, MAPPINGS),
                DirectorySyncHashCalculator.compute(user2, MAPPINGS));
    }

    @Test
    void changedMappedValueProducesDifferentHash() {
        DirectoryUser before = new DirectoryUser(
                "ext-1", "a@test.com", Map.of("firstName", "Alex", "email", "a@test.com"));
        DirectoryUser after = new DirectoryUser(
                "ext-1", "a@test.com", Map.of("firstName", "Alexander", "email", "a@test.com"));
        assertNotEquals(
                DirectorySyncHashCalculator.compute(before, MAPPINGS),
                DirectorySyncHashCalculator.compute(after, MAPPINGS));
    }
}
