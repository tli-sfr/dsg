package com.ringcentral.dsg.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Job retrieval and sync worker poll on separate threads so one consumer's SQS long-poll
 * does not delay the other.
 */
@Configuration
public class SchedulingConfiguration {

    @Bean(name = "dsgTaskScheduler")
    public TaskScheduler dsgTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("dsg-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    @Configuration
    static class DsgSchedulerConfigurer implements SchedulingConfigurer {

        private final TaskScheduler taskScheduler;

        DsgSchedulerConfigurer(@Qualifier("dsgTaskScheduler") TaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
        }

        @Override
        public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
            taskRegistrar.setScheduler(taskScheduler);
        }
    }
}
