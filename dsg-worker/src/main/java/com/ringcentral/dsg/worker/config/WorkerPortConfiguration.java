package com.ringcentral.dsg.worker.config;

import com.ringcentral.dsg.provisioning.RcProvisioningPort;
import com.ringcentral.dsg.worker.stub.StubRcProvisioningPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerPortConfiguration {

    @Bean
    @ConditionalOnMissingBean(RcProvisioningPort.class)
    public RcProvisioningPort rcProvisioningPort() {
        return new StubRcProvisioningPort();
    }
}
