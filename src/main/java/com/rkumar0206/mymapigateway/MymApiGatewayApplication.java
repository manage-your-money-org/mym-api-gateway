package com.rkumar0206.mymapigateway;

import com.rkumar0206.mymapigateway.config.MymRabbitMQConfig;
import com.rkumar0206.mymapigateway.models.CreateBinding;
import com.rkumar0206.mymapigateway.models.CreateExchange;
import com.rkumar0206.mymapigateway.models.CreateQueue;
import com.rkumar0206.mymapigateway.models.TopicPermission;
import com.rkumar0206.mymapigateway.service.RabbitMQDynamicSetupService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigurationProperties(MymRabbitMQConfig.class)
public class MymApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymApiGatewayApplication.class, args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    CommandLineRunner run(MymRabbitMQConfig mymRabbitMQConfig, RabbitMQDynamicSetupService rabbitMQDynamicSetupService) {

        return args -> {

            // Create New RabbitMq User
            rabbitMQDynamicSetupService.createRabbitMqUser();
            // =======================

            // Create a Vhost
            rabbitMQDynamicSetupService.createVhost();
            // ==========================

            //Grant Topic permissions
            TopicPermission topicPermission1 = new TopicPermission("(AMQP default)", ".*", ".*");
            TopicPermission topicPermission2 = new TopicPermission("amq.topic", ".*", ".*");

            rabbitMQDynamicSetupService.createTopicPermission(topicPermission1);
            rabbitMQDynamicSetupService.createTopicPermission(topicPermission2);

            // =======================
            // create an exchange

            CreateExchange createExchange = new CreateExchange("topic", false, true, false);
            rabbitMQDynamicSetupService.createExchange(createExchange);

            // Grant topic permission to new created exchange
            rabbitMQDynamicSetupService.createTopicPermission(new TopicPermission(mymRabbitMQConfig.getMymExchange().getName(), ".*", ".*"));

            // =====================================
            // create a queue

            for (String queue : mymRabbitMQConfig.getMymQueues()) {

                rabbitMQDynamicSetupService.createQueue(queue, new CreateQueue(false, false));
            }

            // =============================

            // create bindings

            for (String binding : mymRabbitMQConfig.getBindings()) {

                CreateBinding createBinding = new CreateBinding(binding);
                rabbitMQDynamicSetupService.createBinding(createBinding, mymRabbitMQConfig.getMymQueues().get(0));
            }
            //======================

            // disable guest user
            rabbitMQDynamicSetupService.disableGuestUser();
        };
    }
}
