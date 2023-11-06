package com.rkumar0206.mymapigateway.service;

import com.rkumar0206.mymapigateway.config.MymRabbitMQConfig;
import com.rkumar0206.mymapigateway.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
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

        String exchangeName = mymRabbitMQConfig.getMymExchange().getName();

        log.info("Creating binding with: exchange = " + exchangeName + " and queue = " + queueName);

        String createBidingUrl = baseRabbitMqUrl + "/bindings/" + mymRabbitMQConfig.getVhost().getName() + "/e/" + exchangeName + "/q/" + queueName;

        HttpEntity<CreateBinding> createBindingHttpEntity1 = new HttpEntity<>(
                createBinding, getHttpHeader()
        );

        ResponseEntity<String> createBindingResponse = restTemplate.exchange(
                createBidingUrl, HttpMethod.POST, createBindingHttpEntity1, String.class
        );

        if (createBindingResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Create binding response: " + createBindingResponse.getStatusCode());
            log.info("Binding created successfully");
        } else {
            log.error("Failed to create binding with response code : " + createBindingResponse.getStatusCode() + " and message: " + createBindingResponse.getBody());
        }
    }

    @Override
    public void createQueue(String queueName, CreateQueue createQueue) {

        log.info("Creating queue: " + queueName);

        String createQueueUrl = baseRabbitMqUrl + "/queues/" + mymRabbitMQConfig.getVhost().getName() + "/" + queueName;

        HttpEntity<CreateQueue> createQueueHttpEntity = new HttpEntity<>(
                createQueue, getHttpHeader()
        );

        ResponseEntity<String> createQueueResponse = restTemplate.exchange(
                createQueueUrl, HttpMethod.PUT, createQueueHttpEntity, String.class
        );

        if (createQueueResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Create queue response: " + createQueueResponse.getStatusCode());
            log.info("Queue : " + queueName + ", created successfully.");
        } else {
            log.error("Failed to create queue with response code : " + createQueueResponse.getStatusCode() + " and message: " + createQueueResponse.getBody());
        }
    }

    @Override
    public void createExchange(CreateExchange createExchange) {

        log.info("Creating exchange: " + mymRabbitMQConfig.getMymExchange().getName());

        String createExchangeUrl = baseRabbitMqUrl + "/exchanges/" + mymRabbitMQConfig.getVhost().getName() + "/" + mymRabbitMQConfig.getMymExchange().getName();

        HttpEntity<CreateExchange> createExchangeHttpEntity = new HttpEntity<>(
                createExchange, getHttpHeader()
        );

        ResponseEntity<String> createExchangeResponse = restTemplate.exchange(
                createExchangeUrl, HttpMethod.PUT, createExchangeHttpEntity, String.class
        );

        if (createExchangeResponse.getStatusCode().is2xxSuccessful()) {

            log.info("Create exchange response : " + createExchangeResponse.getStatusCode());
            log.info("Exchange " + mymRabbitMQConfig.getMymExchange().getName() + ", created successfully");

        } else {
            log.error("Failed to create exchange " + mymRabbitMQConfig.getMymExchange().getName() + " with response code : " + createExchangeResponse.getStatusCode() + " and message: " + createExchangeResponse.getBody());
        }

    }

    @Override
    public void createTopicPermission(TopicPermission topicPermission) {

        log.info("Creating topic permission : " + topicPermission.getExchange() + " for the user");

        String topicPermissionBaseUrl = baseRabbitMqUrl + "/topic-permissions/" + mymRabbitMQConfig.getVhost().getName() + "/" + mymRabbitMQConfig.getNewUser().getUsername();

        HttpEntity<TopicPermission> topicPermissionRequestEntity = new HttpEntity<>(topicPermission, getHttpHeader());

        ResponseEntity<String> topicPermissionResponse = restTemplate.exchange(
                topicPermissionBaseUrl, HttpMethod.PUT, topicPermissionRequestEntity, String.class
        );

        if (topicPermissionResponse.getStatusCode().is2xxSuccessful()) {

            log.info("Create topic permission response : " + topicPermissionResponse.getStatusCode());
            log.info("topic permission - " + topicPermission.getExchange() + " granted successfully.");
        } else {

            log.error("Failed to grant topic permission - " + topicPermission.getExchange() + " with response code : " + topicPermissionResponse.getStatusCode() + " and message: " + topicPermissionResponse.getBody());
        }
    }

    @Override
    public void createVhost() {

        log.info("Creating new Vhost: " + mymRabbitMQConfig.getVhost().getName());

        // Create new Vhost
        String createVHostBaseUrl = baseRabbitMqUrl + "/vhosts/" + mymRabbitMQConfig.getVhost().getName();

        HttpEntity<RabbitMQUser> vhostRequestEntity = new HttpEntity<>(null, getHttpHeader());
        ResponseEntity<String> vhostResponse = restTemplate.exchange(createVHostBaseUrl, HttpMethod.PUT, vhostRequestEntity, String.class);

        if (vhostResponse.getStatusCode().is2xxSuccessful()) {

            log.info("Create Vhost response : " + vhostResponse.getStatusCode());
            log.info("Vhost created successfully.");
        } else {
            log.error("Failed to create Vhost with response code : " + vhostResponse.getStatusCode() + " with message : " + vhostResponse.getBody());
        }
    }

    @Override
    public void createRabbitMqUser() {

        log.info("Creating new rabbitMQ user.");

        // Create rabbitMQ user
        String userCreateUrl = baseRabbitMqUrl + "/users/" + mymRabbitMQConfig.getNewUser().getUsername();

        log.info("createRabbitMqUser : url : " + userCreateUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("guest", "guest");

        // Create a request entity with the user details
        HttpEntity<RabbitMQUser> createUserRequestEntity = new HttpEntity<>(new RabbitMQUser("administrator", mymRabbitMQConfig.getNewUser().getPassword()), headers);

        try {

            ResponseEntity<String> createUserResponse = restTemplate.exchange(userCreateUrl, HttpMethod.PUT, createUserRequestEntity, String.class);

            if (createUserResponse.getStatusCode().is2xxSuccessful()) {

                log.info("createRabbitMqUser : response : " + createUserResponse.getStatusCode());
                log.info("RabbitMq User created successfully.");
            } else {
                log.error("Failed to create rabbitmq user with response code : " + createUserResponse.getStatusCode() + " with message : " + createUserResponse.getBody());
                System.err.println("Failed to create rabbitmq user.");
            }

        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("While creating new user getting UNAUTHORIZED response with guest credentials, this may state that our new rabbitMQ user is already created and we can skip this process.");
            } else {
                throw e;
            }
        }
    }

    @Override
    public void disableGuestUser() {

        log.info("Deleting the guest user");

        String disableGuestUserUrl = baseRabbitMqUrl + "/users/guest";

        HttpEntity<RabbitMQUser> disableGuestUserHttpEntity = new HttpEntity<>(null, getHttpHeader());

        try {
            ResponseEntity<String> disableGuestUserResponse = restTemplate.exchange(
                    disableGuestUserUrl, HttpMethod.DELETE, disableGuestUserHttpEntity, String.class
            );

            if (disableGuestUserResponse.getStatusCode().is2xxSuccessful()) {

                log.info("Guest User deleted successfully.");
            } else {
                log.error("Failed to delete guest user, with response code : " + disableGuestUserResponse.getStatusCode() + " with message : " + disableGuestUserResponse.getBody());
            }
        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("While deleting guest user getting NOT FOUND response, which states that guest user already deleted and we can skip this process");
            }
        }
    }

    private HttpHeaders getHttpHeader() {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(mymRabbitMQConfig.getNewUser().getUsername(), mymRabbitMQConfig.getNewUser().getPassword());

        return headers;
    }

}
