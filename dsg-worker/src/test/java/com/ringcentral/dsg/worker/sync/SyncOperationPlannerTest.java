package com.ringcentral.dsg.worker.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.mapping.AttributeMapping;
import com.ringcentral.dsg.persistence.model.DirectorySyncUserHashRecord;
import com.ringcentral.dsg.persistence.repo.DirectorySyncUserHashRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncOperationPlannerTest {

    private static final String ACCOUNT = "623256020";
    private static final int OKTA_TYPE = 2;
    private static final String EXTERNAL_ID = "00u16ux1qhu3swZ5x1d8";
    private static final List<AttributeMapping> MAPPINGS = List.of(
            new AttributeMapping("profile.firstName", "firstName"),
            new AttributeMapping("profile.lastName", "lastName"),
            new AttributeMapping("profile.email", "email"));

    @Mock
    private DirectorySyncUserHashRepository userHashRepository;

    private SyncOperationPlanner planner;

    @BeforeEach
    void setUp() {
        planner = new SyncOperationPlanner(userHashRepository);
    }

    @Test
    void plansCreateWhenNoPriorHash() {
        when(userHashRepository.find(ACCOUNT, OKTA_TYPE, EXTERNAL_ID)).thenReturn(Optional.empty());

        DirectoryUser user = directoryUser("rc", "test", "tony@test.com");
        SyncOperationPlan plan = planner.plan(ACCOUNT, OKTA_TYPE, user, MAPPINGS);

        assertEquals(SyncOperation.CREATE, plan.operation());
    }

    @Test
    void plansUnchangedWhenHashMatches() {
        DirectoryUser user = directoryUser("rc", "test", "tony@test.com");
        SyncOperationPlan first = planner.plan(ACCOUNT, OKTA_TYPE, user, MAPPINGS);
        when(userHashRepository.find(ACCOUNT, OKTA_TYPE, EXTERNAL_ID))
                .thenReturn(Optional.of(new DirectorySyncUserHashRecord(
                        ACCOUNT, OKTA_TYPE, EXTERNAL_ID, first.externalUserHash(), "5175558020", null)));

        SyncOperationPlan second = planner.plan(ACCOUNT, OKTA_TYPE, user, MAPPINGS);

        assertEquals(SyncOperation.UNCHANGED, second.operation());
        assertEquals("5175558020", second.mailboxId());
    }

    @Test
    void plansUpdateWhenHashDiffers() {
        DirectoryUser user = directoryUser("rc", "test", "tony@test.com");
        SyncOperationPlan first = planner.plan(ACCOUNT, OKTA_TYPE, user, MAPPINGS);
        when(userHashRepository.find(ACCOUNT, OKTA_TYPE, EXTERNAL_ID))
                .thenReturn(Optional.of(new DirectorySyncUserHashRecord(
                        ACCOUNT, OKTA_TYPE, EXTERNAL_ID, "stale-hash", "5175558020", null)));

        SyncOperationPlan plan = planner.plan(ACCOUNT, OKTA_TYPE, user, MAPPINGS);

        assertEquals(SyncOperation.UPDATE, plan.operation());
        assertEquals("5175558020", plan.mailboxId());
        assertEquals(first.externalUserHash(), plan.externalUserHash());
    }

    private static DirectoryUser directoryUser(String first, String last, String email) {
        return new DirectoryUser(
                EXTERNAL_ID,
                email,
                Map.of(
                        "profile.firstName", first,
                        "profile.lastName", last,
                        "profile.email", email));
    }
}
