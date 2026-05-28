package com.ringcentral.dsg.directory;

import java.util.Map;

public record DirectoryUser(String externalId, String email, Map<String, String> attributes) {
}
