package com.rkumar0206.mymapigateway.service;

import com.rkumar0206.mymapigateway.models.CreateBinding;
import com.rkumar0206.mymapigateway.models.CreateExchange;
import com.rkumar0206.mymapigateway.models.CreateQueue;
import com.rkumar0206.mymapigateway.models.TopicPermission;

public interface RabbitMQDynamicSetupService {

    void createBinding(CreateBinding createBinding, String queueName);

    void createQueue(String queueName, CreateQueue createQueue);

    void createExchange(CreateExchange createExchange);

    void createTopicPermission(TopicPermission topicPermission);

    void createVhost();

    void createRabbitMqUser();

}
