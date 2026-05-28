package com.ringcentral.dsg.provisioning;

/**
 * Primary product licenses selectable on provisioning rules.
 */
public enum ProductLicense {
    VIDEO_PRO("VideoPro", "Video Pro", "Meetings only"),
    VIDEO_PRO_PLUS("VideoProPlus", "Video Pro+", "Advanced Meetings"),
    RING_EX("RingEX", "RingEX", "Core phone & messaging");

    private final String licenseId;
    private final String label;
    private final String description;

    ProductLicense(String licenseId, String label, String description) {
        this.licenseId = licenseId;
        this.label = label;
        this.description = description;
    }

    public String licenseId() {
        return licenseId;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public boolean usesExtensionCreateApi() {
        return this == VIDEO_PRO || this == VIDEO_PRO_PLUS;
    }

    public boolean usesScimApi() {
        return this == RING_EX;
    }

    public static ProductLicense fromLicenseId(String licenseId) {
        if (licenseId == null || licenseId.isBlank()) {
            return RING_EX;
        }
        for (ProductLicense license : values()) {
            if (license.licenseId.equalsIgnoreCase(licenseId)
                    || license.name().equalsIgnoreCase(licenseId.replace(" ", "_"))) {
                return license;
            }
        }
        return switch (licenseId) {
            case "Video Pro" -> VIDEO_PRO;
            case "Video Pro+" -> VIDEO_PRO_PLUS;
            default -> RING_EX;
        };
    }
}
