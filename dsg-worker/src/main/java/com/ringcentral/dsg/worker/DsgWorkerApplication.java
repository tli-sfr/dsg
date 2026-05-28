package com.ringcentral.dsg.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.ringcentral.dsg.worker", "com.ringcentral.dsg.persistence"})
@EnableScheduling
public class DsgWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DsgWorkerApplication.class, args);
    }
}
