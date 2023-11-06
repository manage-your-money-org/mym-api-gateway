package com.rkumar0206.mymapigateway.service;

import com.rkumar0206.mymapigateway.config.MymRabbitMQConfig;
import com.rkumar0206.mymapigateway.models.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RabbitMQDynamicSetupServiceImpl implements RabbitMQDynamicSetupService {

    private final MymRabbitMQConfig mymRabbitMQConfig;
    private final RestTemplate restTemplate;
    private final String baseRabbitMqUrl;

    public RabbitMQDynamicSetupServiceImpl(MymRabbitMQConfig mymRabbitMQConfig, RestTemplate restTemplate) {
        this.mymRabbitMQConfig = mymRabbitMQConfig;
        this.restTemplate = restTemplate;

        baseRabbitMqUrl = "http://" + mymRabbitMQConfig.getMqHost() + ":" + mymRabbitMQConfig.getMqPort() + "/api";
    }


    @Override
    public void createBinding(CreateBinding createBinding, String queueName) {

        String createBidingUrl = baseRabbitMqUrl + "/bindings/" + mymRabbitMQConfig.getVhost().getName() + "/e/" + mymRabbitMQConfig.getMymExchange().getName() + "/q/" + queueName;

        HttpEntity<CreateBinding> createBindingHttpEntity1 = new HttpEntity<>(
                createBinding, getHttpHeader()
        );

        ResponseEntity<String> createBindingResponse = restTemplate.exchange(
                createBidingUrl, HttpMethod.POST, createBindingHttpEntity1, String.class
        );

        if (createBindingResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("Binding with routing key - " + createBinding.getRouting_key() + " created successfully");
        } else {
            System.err.println("Failed to create binding");
        }
    }

    @Override
    public void createQueue(String queueName, CreateQueue createQueue) {

        String createQueueUrl = baseRabbitMqUrl + "/queues/" + mymRabbitMQConfig.getVhost().getName() + "/" + queueName;

        HttpEntity<CreateQueue> createQueueHttpEntity = new HttpEntity<>(
                createQueue, getHttpHeader()
        );

        ResponseEntity<String> createQueueResponse = restTemplate.exchange(
                createQueueUrl, HttpMethod.PUT, createQueueHttpEntity, String.class
        );

        if (createQueueResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("Queue creating successful");
        } else {
            System.err.println("Failed to create queue");
        }
    }

    @Override
    public void createExchange(CreateExchange createExchange) {

        String createExchangeUrl = baseRabbitMqUrl + "/exchanges/" + mymRabbitMQConfig.getVhost().getName() + "/" + mymRabbitMQConfig.getMymExchange().getName();

        HttpEntity<CreateExchange> createExchangeHttpEntity = new HttpEntity<>(
                createExchange, getHttpHeader()
        );

        ResponseEntity<String> createExchangeResponse = restTemplate.exchange(
                createExchangeUrl, HttpMethod.PUT, createExchangeHttpEntity, String.class
        );

        if (createExchangeResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("Exchange creating successful");
        } else {
            System.err.println("Failed to create exchange");
        }

    }

    @Override
    public void createTopicPermission(TopicPermission topicPermission) {

        String topicPermissionBaseUrl = baseRabbitMqUrl + "/topic-permissions/" + mymRabbitMQConfig.getVhost().getName() + "/" + mymRabbitMQConfig.getNewUser().getUsername();

        HttpEntity<TopicPermission> topicPermissionRequestEntity = new HttpEntity<>(topicPermission, getHttpHeader());

        ResponseEntity<String> topicPermissionResponse1 = restTemplate.exchange(
                topicPermissionBaseUrl, HttpMethod.PUT, topicPermissionRequestEntity, String.class
        );

        if (topicPermissionResponse1.getStatusCode().is2xxSuccessful()) {
            System.out.println("topic permission - " + topicPermission.getExchange() + " granted successfully.");
        } else {
            System.err.println("Failed to grant topic permission - " + topicPermission.getExchange());
        }
    }

    @Override
    public void createVhost() {

        // Create new Vhost
        String createVHostBaseUrl = baseRabbitMqUrl + "/vhosts/" + mymRabbitMQConfig.getVhost().getName();

        HttpEntity<RabbitMQUser> vhostRequestEntity = new HttpEntity<>(null, getHttpHeader());
        ResponseEntity<String> vhostResponse = restTemplate.exchange(createVHostBaseUrl, HttpMethod.PUT, vhostRequestEntity, String.class);

        if (vhostResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("Vhost created successfully.");
        } else {
            System.err.println("Failed to create vhost.");
        }
    }

    @Override
    public void createRabbitMqUser() {

        // Create rabbitMQ user
        String userCreateUrl = baseRabbitMqUrl + "/users/" + mymRabbitMQConfig.getNewUser().getUsername();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("guest", "guest");

        // Create a request entity with the user details
        HttpEntity<RabbitMQUser> createUserRequestEntity = new HttpEntity<>(new RabbitMQUser("administrator", mymRabbitMQConfig.getNewUser().getPassword()), headers);
        ResponseEntity<String> createUserResponse = restTemplate.exchange(userCreateUrl, HttpMethod.PUT, createUserRequestEntity, String.class);

        if (createUserResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("RabbitMq User created successfully.");
        } else {
            System.err.println("Failed to create rabbitmq user.");
        }
    }

    @Override
    public void disableGuestUser() {

        String disableGuestUserUrl = baseRabbitMqUrl + "/users/guest";

        HttpEntity<RabbitMQUser> disableGuestUserHttpEntity = new HttpEntity<>(null, getHttpHeader());
        ResponseEntity<String> disableGuestUserResponse = restTemplate.exchange(
                disableGuestUserUrl, HttpMethod.DELETE, disableGuestUserHttpEntity, String.class
        );

        if (disableGuestUserResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("Guest User disabled successfully.");
        } else {
            System.err.println("Failed to disable guest user.");
        }
    }

    private HttpHeaders getHttpHeader() {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(mymRabbitMQConfig.getNewUser().getUsername(), mymRabbitMQConfig.getNewUser().getPassword());

        return headers;
    }

}
