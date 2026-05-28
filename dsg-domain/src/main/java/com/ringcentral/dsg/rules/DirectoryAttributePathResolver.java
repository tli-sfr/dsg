package com.ringcentral.dsg.rules;

import com.ringcentral.dsg.directory.DirectoryUser;

public final class DirectoryAttributePathResolver {

    private DirectoryAttributePathResolver() {}

    /**
     * Resolves paths such as {@code user.department} against {@link DirectoryUser#attributes()}.
     */
    public static String resolve(DirectoryUser user, String attributePath) {
        if (attributePath == null || attributePath.isBlank()) {
            return null;
        }
        String key = attributePath.startsWith("user.") ? attributePath.substring("user.".length()) : attributePath;
        return user.attributes().get(key);
    }
}
