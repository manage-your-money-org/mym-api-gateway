package com.rkumar0206.mymapigateway.filters;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkumar0206.mymapigateway.constants.Constants;
import com.rkumar0206.mymapigateway.constants.ErrorMessageConstants;
import com.rkumar0206.mymapigateway.models.UserInfo;
import com.rkumar0206.mymapigateway.utility.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.rkumar0206.mymapigateway.constants.Constants.BEARER;

@Component
@Slf4j
public class AuthorizationFilter implements GlobalFilter {

    @Value("${token.secret}")
    private String tokenSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestUrl = exchange.getRequest().getPath().toString();

        log.info("request-url: " + requestUrl);

        //if the endpoint corresponds to user service, just call the user service
        if (requestUrl.contains("/mym/app/users/login") || requestUrl.contains("/mym/api/users/") || requestUrl.contains("/actuator")) {

            log.info("Authorization header not required or it will be verified in mym-user-authentication-service");

            return chain.filter(exchange);
        } else {

            log.info("Checking for authorization header");

            //check if authorization header is present or not
            List<String> tempAuthHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

            if (tempAuthHeader != null && !tempAuthHeader.isEmpty()) {

                String authorizationHeader = tempAuthHeader.get(0);

                if (!authorizationHeader.startsWith(BEARER)) {

                    return Utility.onError(exchange, ErrorMessageConstants.INVALID_AUTH_TOKEN);
                }

                Algorithm algorithm = Algorithm.HMAC256(tokenSecret.getBytes());

                String token = authorizationHeader.substring(Constants.BEARER.length());

                try {
                    DecodedJWT decodedJWT = Utility.isTokenValid(token, algorithm);

                    String emailId = decodedJWT.getSubject();
                    Claim uid = decodedJWT.getClaim("uid");
                    Claim name = decodedJWT.getClaim("name");
                    Claim isAccountVerified = decodedJWT.getClaim("isAccountVerified");

                    if (decodedJWT.getKeyId() == null
                            || emailId == null
                            || emailId.trim().isEmpty()
                            || decodedJWT.getKeyId().trim().isEmpty()
                            || uid == null || uid.asString().trim().isEmpty()
                            || name == null || name.asString().trim().isEmpty()
                            || isAccountVerified == null

                    ) {
                        return Utility.onError(exchange, ErrorMessageConstants.INVALID_AUTH_TOKEN);
                    }

                    if (!isAccountVerified.asBoolean()) {
                        return Utility.onError(exchange, ErrorMessageConstants.ACCOUNT_NOT_VERIFIED_ERROR);
                    }

                    UserInfo userInfo = new UserInfo(
                            name.asString(),
                            emailId,
                            uid.asString(),
                            isAccountVerified.asBoolean()
                    );

                    ServerWebExchange.Builder request = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header(Constants.USER_INFO_HEADER_NAME, new ObjectMapper().writeValueAsString(userInfo))
                                    .build());

                    return chain.filter(request.build());

                } catch (Exception e) {

                    log.info("Exception occurred while checking authorization token: " + e.getMessage());
                    return Utility.onError(exchange, e.getMessage());
                }
            } else {
                return Utility.onError(exchange, ErrorMessageConstants.NO_AUTH_HEADER_PRESENT_ERROR);
            }
        }
    }
}
