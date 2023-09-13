package com.rkumar0206.mymapigateway.filters;

import com.rkumar0206.mymapigateway.constants.ErrorMessageConstants;
import com.rkumar0206.mymapigateway.feignClients.UserAuthenticationFeignClient;
import com.rkumar0206.mymapigateway.models.CustomResponse;
import com.rkumar0206.mymapigateway.models.UserAccountResponse;
import com.rkumar0206.mymapigateway.utility.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthorizationFilter implements GlobalFilter {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private UserAuthenticationFeignClient userAuthenticationFeignClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestUrl = exchange.getRequest().getPath().toString();

        //if the endpoint corresponds to user service, just call the user service
        if (requestUrl.contains("/mym/app/users/login") || requestUrl.contains("/mym/api/users/")) {

            return chain.filter(exchange);
        } else {

            //check if authorization header is present or not
            List<String> tempAuthHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

            if (tempAuthHeader != null && !tempAuthHeader.isEmpty()) {

                String authorizationHeader = tempAuthHeader.get(0);

                if (!authorizationHeader.startsWith("Bearer ")) {

                    return Utility.onError(exchange, ErrorMessageConstants.INVALID_AUTH_TOKEN);
                }

                //if authorization token is present call the user service and get the user details
                try {
                    ResponseEntity<CustomResponse<UserAccountResponse>> userDetailsResponse =
                            userAuthenticationFeignClient.getUserDetails(authorizationHeader);

                    if (userDetailsResponse.getStatusCode() == HttpStatus.OK
                            && userDetailsResponse.getBody() != null) {

                        ServerHttpResponse response = exchange.getResponse();

                        response.getHeaders().add("uid", userDetailsResponse.getBody().getBody().getUid());
                        return chain.filter(exchange);
                    } else {

                        return Utility.onError(exchange, ErrorMessageConstants.USER_NOT_AUTHORIZED);
                    }
                } catch (Exception e) {

                    return Utility.onError(exchange, e.getMessage());
                }

            } else {
                return Utility.onError(exchange, ErrorMessageConstants.NO_AUTH_HEADER_PRESENT_ERROR);
            }
        }
    }
}
