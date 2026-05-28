package com.ringcentral.dsg.worker.config;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;
import com.ringcentral.dsg.worker.stub.StubDirectoryPort;
import com.ringcentral.dsg.worker.stub.StubRcProvisioningPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerPortConfiguration {

    @Bean
    public DirectoryPort directoryPort() {
        return new StubDirectoryPort();
    }

    @Bean
    public RcProvisioningPort rcProvisioningPort() {
        return new StubRcProvisioningPort();
    }
}
