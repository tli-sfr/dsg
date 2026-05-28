package com.ringcentral.dsg.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.sqs.SqsMessageQueuePort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingAutoConfiguration {

  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean
  public MessageQueuePort messageQueuePort(
      MessagingProperties properties, ObjectMapper objectMapper) {
    return new SqsMessageQueuePort(properties, objectMapper);
  }
}
