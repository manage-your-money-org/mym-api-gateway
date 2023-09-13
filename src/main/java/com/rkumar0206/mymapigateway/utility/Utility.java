package com.rkumar0206.mymapigateway.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Utility {

    public static Mono<Void> onError(ServerWebExchange exchange, String error) {

        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("code", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        errorResponse.put("error", error);

        try {
            error = new ObjectMapper().writer()
                    .withDefaultPrettyPrinter().writeValueAsString(errorResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        DataBuffer dataBuffer = response.bufferFactory().wrap(error.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(dataBuffer));
    }
}
