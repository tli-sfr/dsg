package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * RingCentral provisioning HTTP client.
 *
 * @see <a href="https://developers.ringcentral.com/api-reference/Extensions/createExtension">createExtension</a>
 * @see <a href="https://developers.ringcentral.com/api-reference/Extensions/bulkAssignExtensions">bulkAssignExtensions</a>
 * @see <a href="https://developers.ringcentral.com/api-reference/User-Settings/updateExtension">updateExtension</a>
 */
@Component
public class RcProvisioningClient {

    private final RestTemplate restTemplate;
    private final RcOAuthProperties properties;
    private final ObjectMapper objectMapper;

    public RcProvisioningClient(
            RestTemplate rcOAuthRestTemplate,
            RcOAuthProperties properties,
            ObjectMapper objectMapper) {
        this.restTemplate = rcOAuthRestTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public RcExtensionResponse createExtension(String accessToken, RcExtensionCreateRequest request) {
        String url = properties.restApiBase() + "/account/~/extension";
        return exchange(HttpMethod.POST, url, accessToken, request, RcExtensionResponse.class);
    }

    public RcExtensionResponse updateExtension(
            String accessToken, String extensionId, RcExtensionUpdateRequest request) {
        String url = properties.restApiBase() + "/account/~/extension/" + extensionId;
        return exchange(HttpMethod.PUT, url, accessToken, request, RcExtensionResponse.class);
    }

    public RcBulkAssignHttpResult bulkAssignExtensions(String accessToken, RcBulkAssignRequest request) {
        String url = properties.restApiBase() + "/account/~/extension/bulk-assign";
        String rawBody = exchangeRaw(HttpMethod.POST, url, accessToken, request);
        try {
            RcBulkAssignResponse parsed = objectMapper.readValue(rawBody, RcBulkAssignResponse.class);
            return new RcBulkAssignHttpResult(rawBody, parsed);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "RingCentral bulk-assign returned unparseable JSON: " + rawBody, ex);
        }
    }

    private String exchangeRaw(HttpMethod method, String url, String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, method, new HttpEntity<>(body, headers), String.class);
            return response.getBody() != null ? response.getBody() : "";
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "RingCentral request failed (" + ex.getStatusCode().value() + "): "
                            + ex.getResponseBodyAsString(),
                    ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("RingCentral request failed: " + ex.getMessage(), ex);
        }
    }

    private <T> T exchange(
            HttpMethod method,
            String url,
            String accessToken,
            Object body,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        try {
            return restTemplate
                    .exchange(url, method, new HttpEntity<>(body, headers), responseType)
                    .getBody();
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "RingCentral request failed (" + ex.getStatusCode().value() + "): "
                            + ex.getResponseBodyAsString(),
                    ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("RingCentral request failed: " + ex.getMessage(), ex);
        }
    }
}
