package com.ringcentral.dsg.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Single DSG backend process: configuration REST API plus runtime pipeline
 * (job retrieval, sync worker, consolidator) as {@code @Scheduled} consumers.
 *
 * @see docs/architecture/sync-runtime.md
 */
@SpringBootApplication(scanBasePackages = {
        "com.ringcentral.dsg.api",
        "com.ringcentral.dsg.worker",
        "com.ringcentral.dsg.persistence"
})
@EnableScheduling
public class DsgApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DsgApiApplication.class, args);
    }
}
