package com.ringcentral.dsg.api.rc;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RcApiClient {

    private final RestTemplate restTemplate;
    private final RcOAuthProperties properties;

    public RcApiClient(RestTemplate rcOAuthRestTemplate, RcOAuthProperties properties) {
        this.restTemplate = rcOAuthRestTemplate;
        this.properties = properties;
    }

    /**
     * @see <a href="https://developers.ringcentral.com/api-reference/User-Settings/readExtension">readExtension</a>
     */
    public RcExtensionResponse readCurrentExtension(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            return restTemplate.exchange(
                            properties.readExtensionEndpoint(),
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            RcExtensionResponse.class)
                    .getBody();
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "RingCentral readExtension failed (" + ex.getStatusCode().value() + "): "
                            + ex.getResponseBodyAsString(),
                    ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("RingCentral readExtension failed: " + ex.getMessage(), ex);
        }
    }
}
