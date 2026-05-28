package com.ringcentral.dsg.api.config;

import com.ringcentral.dsg.api.directory.DirectoryIdpAccessTokenService;
import com.ringcentral.dsg.api.directory.DirectoryIdpOAuthClient;
import com.ringcentral.dsg.api.directory.OktaDirectoryPort;
import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import com.ringcentral.dsg.worker.stub.StubDirectoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectoryPortConfiguration {

    @Bean
    @ConditionalOnProperty(name = "dsg.directory.stub", havingValue = "false", matchIfMissing = true)
    public DirectoryPort directoryPort(
            AccountDirectoryOauthRepository oauthRepository,
            DirectoryIdpAccessTokenService accessTokenService,
            DirectoryIdpOAuthClient idpOAuthClient) {
        return new OktaDirectoryPort(oauthRepository, accessTokenService, idpOAuthClient);
    }

    @Bean
    @ConditionalOnProperty(name = "dsg.directory.stub", havingValue = "true")
    public DirectoryPort directoryPortStub() {
        return new StubDirectoryPort();
    }
}
