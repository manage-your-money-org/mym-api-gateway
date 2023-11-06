package com.rkumar0206.mymapigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "mymrabbitmq")
public class MymRabbitMQConfig {
    private NewUser newUser;
    private Vhost vhost;
    private MymExchange mymExchange;
    private List<String> mymQueues;
    private List<String> bindings;
    private String mqHost;
    private Integer mqPort;

    // Getters and setters for the properties

    public NewUser getNewUser() {
        return newUser;
    }

    public void setNewUser(NewUser newUser) {
        this.newUser = newUser;
    }

    public Vhost getVhost() {
        return vhost;
    }

    public void setVhost(Vhost vhost) {
        this.vhost = vhost;
    }

    public MymExchange getMymExchange() {
        return mymExchange;
    }

    public void setMymExchange(MymExchange mymExchange) {
        this.mymExchange = mymExchange;
    }

    public String getMqHost() {
        return mqHost;
    }

    public void setMqHost(String mqHost) {
        this.mqHost = mqHost;
    }

    public Integer getMqPort() {
        return mqPort;
    }

    public void setMqPort(Integer mqPort) {
        this.mqPort = mqPort;
    }

    public List<String> getMymQueues() {
        return mymQueues;
    }

    public void setMymQueues(List<String> mymQueues) {
        this.mymQueues = mymQueues;
    }

    public List<String> getBindings() {
        return bindings;
    }

    public void setBindings(List<String> bindings) {
        this.bindings = bindings;
    }

    // Other classes for nested properties
    public static class NewUser {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Vhost {
        private String name;
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class MymExchange {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
