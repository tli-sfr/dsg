package com.ringcentral.dsg.mapping;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * SHA-256 hash over mapped RC attribute values (fields covered by {@code attribute_mapping}).
 */
public final class DirectorySyncHashCalculator {

    private DirectorySyncHashCalculator() {}

    public static String compute(DirectoryUser mappedUser, List<AttributeMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return sha256("");
        }
        Map<String, String> canonical = new TreeMap<>();
        Map<String, String> attrs = mappedUser.attributes();
        for (AttributeMapping mapping : mappings) {
            String rcName = mapping.rcAttributeName();
            String value = attrs.get(rcName);
            canonical.put(rcName, value != null ? value : "");
        }
        String payload = canonical.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        return sha256(payload);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
