package com.ringcentral.dsg.api.directory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    public DirectoryIdpUserProfile fetchOktaUserProfile(String oktaDomain, String accessToken) {
        String url = trimTrailingSlash(oktaDomain) + "/oauth2/v1/userinfo";
        return getUserProfile(url, accessToken);
    }

    public DirectoryIdpUserProfile fetchAzureUserProfile(String accessToken) {
        return getUserProfile("https://graph.microsoft.com/v1.0/me", accessToken);
    }

    public List<DirectoryIdpGroup> searchOktaGroups(String oktaDomain, String accessToken, String query) {
        String url = UriComponentsBuilder.fromHttpUrl(trimTrailingSlash(oktaDomain) + "/api/v1/groups")
                .queryParam("q", query)
                .queryParam("limit", 25)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            OktaGroupItem[] items = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), OktaGroupItem[].class)
                    .getBody();
            if (items == null) {
                return List.of();
            }
            List<DirectoryIdpGroup> groups = new ArrayList<>();
            for (OktaGroupItem item : items) {
                if (item.id() != null && item.profile() != null && item.profile().name() != null) {
                    groups.add(new DirectoryIdpGroup(item.id(), item.profile().name()));
                }
            }
            return groups;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Okta group search failed: " + ex.getMessage(), ex);
        }
    }

    public List<DirectoryIdpGroup> searchAzureGroups(String accessToken, String query) {
        String escaped = query.replace("'", "''");
        String url = UriComponentsBuilder
                .fromHttpUrl("https://graph.microsoft.com/v1.0/groups")
                .queryParam("$filter", "startswith(displayName,'" + escaped + "')")
                .queryParam("$select", "id,displayName")
                .queryParam("$top", 25)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            AzureGroupsResponse response = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), AzureGroupsResponse.class)
                    .getBody();
            if (response == null || response.value() == null) {
                return List.of();
            }
            return response.value().stream()
                    .filter(item -> item.id() != null && item.displayName() != null)
                    .map(item -> new DirectoryIdpGroup(item.id(), item.displayName()))
                    .toList();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Azure group search failed: " + ex.getMessage(), ex);
        }
    }

    private DirectoryIdpUserProfile getUserProfile(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            return restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), DirectoryIdpUserProfile.class)
                    .getBody();
        } catch (RestClientException ex) {
            return null;
        }
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
