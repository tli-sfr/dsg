package com.ringcentral.dsg.api.rc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dsg.rc")
public class RcOAuthProperties {

    private String serverUrl = "https://platform.devtest.ringcentral.com";
    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "http://localhost:5173/oauth/callback";

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public String tokenEndpoint() {
        return trimTrailingSlash(serverUrl) + "/restapi/oauth/token";
    }

    public String authorizeEndpoint() {
        return trimTrailingSlash(serverUrl) + "/restapi/oauth/authorize";
    }

    public String readExtensionEndpoint() {
        return trimTrailingSlash(serverUrl) + "/restapi/v1.0/account/~/extension/~";
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
