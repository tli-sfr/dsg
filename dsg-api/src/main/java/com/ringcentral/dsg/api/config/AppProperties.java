package com.ringcentral.dsg.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dsg.app")
public class AppProperties {

    private String publicBaseUrl = "http://localhost:5173";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String directoryOAuthCallbackUrl() {
        String base = trimTrailingSlash(publicBaseUrl);
        return base + "/directory-integration/oauth/callback";
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
