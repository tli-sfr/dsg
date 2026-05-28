package com.ringcentral.dsg.api.directory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class DirectoryIdpOAuthClient {

    private final RestTemplate restTemplate;

    public DirectoryIdpOAuthClient(RestTemplate rcOAuthRestTemplate) {
        this.restTemplate = rcOAuthRestTemplate;
    }

    public String buildAzureAuthorizeUrl(
            String tenantId,
            String clientId,
            String redirectUri,
            String scopes,
            String state) {
        String scope = scopes != null && !scopes.isBlank()
                ? scopes
                : "offline_access openid https://graph.microsoft.com/Group.Read.All https://graph.microsoft.com/User.Read.All";
        return "https://login.microsoftonline.com/" + encodePath(tenantId) + "/oauth2/v2.0/authorize"
                + "?client_id=" + encode(clientId)
                + "&response_type=code"
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(scope)
                + "&state=" + encode(state);
    }

    public String buildOktaAuthorizeUrl(
            String oktaDomain,
            String clientId,
            String redirectUri,
            String scopes,
            String state) {
        String base = trimTrailingSlash(oktaDomain);
        String scope = scopes != null && !scopes.isBlank()
                ? scopes
                : "openid offline_access okta.groups.read okta.users.read";
        return base + "/oauth2/v1/authorize"
                + "?client_id=" + encode(clientId)
                + "&response_type=code"
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(scope)
                + "&state=" + encode(state);
    }

    public DirectoryIdpTokenResponse exchangeAzureCode(
            String tenantId,
            String clientId,
            String clientSecret,
            String redirectUri,
            String code) {
        String tokenUrl = "https://login.microsoftonline.com/" + encodePath(tenantId) + "/oauth2/v2.0/token";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        return postToken(tokenUrl, body);
    }

    public DirectoryIdpTokenResponse exchangeOktaCode(
            String oktaDomain,
            String clientId,
            String clientSecret,
            String redirectUri,
            String code) {
        String tokenUrl = trimTrailingSlash(oktaDomain) + "/oauth2/v1/token";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        return postToken(tokenUrl, body);
    }

    private DirectoryIdpTokenResponse postToken(String tokenUrl, MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            return restTemplate.postForObject(tokenUrl, new HttpEntity<>(body, headers), DirectoryIdpTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "Directory token request failed (" + ex.getStatusCode().value() + "): "
                            + ex.getResponseBodyAsString(),
                    ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Directory token request failed: " + ex.getMessage(), ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String encodePath(String value) {
        return value;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
