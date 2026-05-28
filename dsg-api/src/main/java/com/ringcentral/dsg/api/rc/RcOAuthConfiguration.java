package com.ringcentral.dsg.api.rc;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(RcOAuthProperties.class)
public class RcOAuthConfiguration {

    @Bean
    public RestTemplate rcOAuthRestTemplate() {
        return new RestTemplate();
    }
}
