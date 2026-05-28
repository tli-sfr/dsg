package com.ringcentral.dsg.api.rc;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * RingCentral provisioning HTTP client.
 *
 * @see <a href="https://developers.ringcentral.com/api-reference/Extensions/createExtension">createExtension</a>
 * @see <a href="https://developers.ringcentral.com/api-reference/SCIM/scimGetUser2">scimGetUser2</a>
 * @see <a href="https://developers.ringcentral.com/api-reference/User-Settings/updateExtension">updateExtension</a>
 */
@Component
public class RcProvisioningClient {

    private final RestTemplate restTemplate;
    private final RcOAuthProperties properties;

    public RcProvisioningClient(RestTemplate rcOAuthRestTemplate, RcOAuthProperties properties) {
        this.restTemplate = rcOAuthRestTemplate;
        this.properties = properties;
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

    public RcScimUserResponse createScimUser(String accessToken, RcScimUserRequest request) {
        String url = properties.scimApiBase() + "/Users";
        return exchange(HttpMethod.POST, url, accessToken, request, RcScimUserResponse.class);
    }

    /**
     * @see <a href="https://developers.ringcentral.com/api-reference/SCIM/scimGetUser2">scimGetUser2</a>
     */
    public RcScimUserResponse getScimUser(String accessToken, String scimUserId) {
        String url = properties.scimApiBase() + "/Users/" + scimUserId;
        return exchange(HttpMethod.GET, url, accessToken, null, RcScimUserResponse.class);
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
