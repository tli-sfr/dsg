package com.ringcentral.dsg.api.directory;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.LinkedHashMap;
import java.util.Map;

final class OktaDirectoryUserMapper {

    private OktaDirectoryUserMapper() {}

    static DirectoryUser toDirectoryUser(OktaUserItem item) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Map<String, Object> profile = item.profile();
        if (profile != null) {
            for (Map.Entry<String, Object> entry : profile.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String text = entry.getValue().toString().trim();
                if (!text.isEmpty()) {
                    attributes.put("profile." + entry.getKey(), text);
                }
            }
        }
        String email = firstNonBlank(
                attributes.get("profile.email"),
                attributes.get("profile.login"),
                attributes.get("profile.secondEmail"));
        return new DirectoryUser(item.id(), email, Map.copyOf(attributes));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
