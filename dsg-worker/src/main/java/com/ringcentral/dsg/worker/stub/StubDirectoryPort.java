package com.ringcentral.dsg.worker.stub;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.List;
import java.util.Map;

public class StubDirectoryPort implements DirectoryPort {

    @Override
    public List<DirectoryUser> listGroupMembers(String accountId, String directoryGroupId) {
        String group = directoryGroupId != null ? directoryGroupId : "default-group";
        return List.of(
                new DirectoryUser(
                        accountId + "-user-1",
                        accountId + "-user-1@example.com",
                        Map.of("department", "Sales", "group", group)),
                new DirectoryUser(
                        accountId + "-user-2",
                        accountId + "-user-2@example.com",
                        Map.of("department", "Engineering", "group", group)));
    }
}
