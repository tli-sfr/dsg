package com.ringcentral.dsg.directory;

import java.util.List;

/**
 * Vendor directory adapter port (ADR-004). Phase 1 uses stub implementations.
 */
public interface DirectoryPort {

    List<DirectoryUser> listGroupMembers(String accountId, String directoryGroupId);
}
