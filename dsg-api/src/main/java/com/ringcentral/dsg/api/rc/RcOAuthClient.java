package com.ringcentral.dsg.api.rc;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class RcOAuthClient {

    private final RestTemplate restTemplate;
    private final RcOAuthProperties properties;

    public RcOAuthClient(RestTemplate rcOAuthRestTemplate, RcOAuthProperties properties) {
        this.restTemplate = rcOAuthRestTemplate;
        this.properties = properties;
    }

    public RcTokenResponse exchangeAuthorizationCode(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        return postToken(body);
    }

    public RcTokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        return postToken(body);
    }

    private RcTokenResponse postToken(MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader());
        try {
            return restTemplate.postForObject(
                    properties.tokenEndpoint(),
                    new HttpEntity<>(body, headers),
                    RcTokenResponse.class);
        } catch (HttpStatusCodeException ex) {
            String rcBody = ex.getResponseBodyAsString();
            throw new IllegalStateException(
                    "RingCentral token request failed (" + ex.getStatusCode().value() + "): "
                            + summarizeRcError(rcBody),
                    ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("RingCentral token request failed: " + ex.getMessage(), ex);
        }
    }

    private static String summarizeRcError(String rcBody) {
        if (rcBody == null || rcBody.isBlank()) {
            return "empty response body";
        }
        if (rcBody.contains("invalid_grant")) {
            return "invalid_grant — authorization code expired, already used, or redirect URI mismatch "
                    + "(ensure dsg.rc.redirect-uri exactly matches your RC app registration)";
        }
        if (rcBody.length() > 300) {
            return rcBody.substring(0, 300) + "...";
        }
        return rcBody;
    }

    private String basicAuthHeader() {
        String credentials = properties.getClientId() + ":" + properties.getClientSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
