package com.rkumar0206.mymapigateway.filters;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkumar0206.mymapigateway.constants.Constants;
import com.rkumar0206.mymapigateway.constants.ErrorMessageConstants;
import com.rkumar0206.mymapigateway.models.UserInfo;
import com.rkumar0206.mymapigateway.utility.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.rkumar0206.mymapigateway.constants.Constants.BEARER;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthorizationFilter implements GlobalFilter {

    private final DiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;
    @Value("${token.secret}")
    private String tokenSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String requestUrl = exchange.getRequest().getPath().toString();

        log.info("request-url: " + requestUrl);

        //if the endpoint corresponds to user service, just call the user service
        if (requestUrl.contains("/mym/app/users/login") || requestUrl.contains("/mym/api/users/") || requestUrl.contains("/actuator")) {

            log.info("Authorization header not required or it is for mym-user-authentication-service");

            return chain.filter(exchange);
        } else {

            // check if tokens are present in cookies or not
            // if present:
            //      1. get the access token and validate
            //      2. if token is valid allow user to navigate further
            //      3. if access token is not valid or expired
            //              1. get the refresh token and validate
            //              2. if refresh token is valid then
            //                  a. then send a request to user-authentication-service refresh token endpoint
            //                  b. if the response is successful
            //                  c. tokens are added to cookies
            //                  d. check if access token added to cookie is different from old access token
            //                  e. if it id different the allow user to navigate further
            //              3. if refresh token is not valid then send 401 response
            // if not present:
            //      1. check if the authorization header is present or not
            //      2. if present
            //          1. validate the token and send the appropriate response

            MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
            boolean isTokenReceivedInCookies = cookies.containsKey(Constants.ACCESS_TOKEN);

            if (isTokenReceivedInCookies) {

                return handleAuthenticationByCookie(exchange, chain, cookies);
            } else {

                return handleAuthenticatedByAuthorizationHeader(exchange, chain);
            }
        }
    }

    private Mono<Void> handleAuthenticatedByAuthorizationHeader(ServerWebExchange exchange, GatewayFilterChain chain) {

        log.info("Checking for authorization header");

        //check if authorization header is present or not
        List<String> tempAuthHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

        if (tempAuthHeader != null && !tempAuthHeader.isEmpty()) {

            String authorizationHeader = tempAuthHeader.get(0);

            if (!authorizationHeader.startsWith(BEARER)) {

                return Utility.onError(exchange, ErrorMessageConstants.INVALID_AUTH_TOKEN);
            }

            String token = authorizationHeader.substring(Constants.BEARER.length());

            try {
                return validateAuthenticationToken(exchange, chain, token)
                        .then(Mono.fromRunnable(() -> {

                            ServerHttpResponse response = exchange.getResponse();

                            ResponseCookie accessTokenCookie = ResponseCookie.from(Constants.ACCESS_TOKEN, token)
                                    .httpOnly(true)
                                    .path("/")
                                    .build();

                            response.addCookie(accessTokenCookie);

                        }));
            } catch (JWTVerificationException ex) {
                return Utility.onError(exchange, ex.getMessage());
            }
        } else {
            return Utility.onError(exchange, ErrorMessageConstants.NO_AUTH_HEADER_PRESENT_ERROR);
        }
    }

    private Mono<Void> handleAuthenticationByCookie(ServerWebExchange exchange, GatewayFilterChain chain, MultiValueMap<String, HttpCookie> cookies) {

        String accessToken = cookies.get(Constants.ACCESS_TOKEN).get(0).getValue();
        String refreshToken = cookies.get(Constants.REFRESH_TOKEN).get(0).getValue();

        try {
            Mono<Void> validatedTokenMono = validateAuthenticationToken(exchange, chain, accessToken);
            return addCookiesInResponse(exchange, validatedTokenMono, accessToken, refreshToken);
        } catch (JWTVerificationException e) {

            if (e.getMessage().startsWith("The Token has expired")) {

                if (!refreshToken.isEmpty()) {

                    // sending response to mym-user-authentication-service to get new access token using the existing refresh token
                    Mono<ResponseEntity<String>> responseEntityMono = sendRequestToUserAuthenticationServiceForNewAccessToken(accessToken, refreshToken);

                    return responseEntityMono.flatMap((resp) -> {
                        if (resp == null) {
                            return Utility.onError(exchange, "Unable to generate new access token");
                        } else if (resp.getStatusCode() == HttpStatus.OK) {
                            return addNewAccessTokenToRequest(exchange, chain, resp, accessToken, refreshToken);
                        } else {
                            return Utility.onError(exchange, "Refresh token invalid: " + resp.getBody());
                        }
                    });
                } else {
                    return Utility.onError(exchange, "Refresh token not found in the cookies");
                }
            } else {
                return Utility.onError(exchange, e.getMessage());
            }
        }
    }

    private Mono<? extends Void> addNewAccessTokenToRequest(ServerWebExchange exchange, GatewayFilterChain chain, ResponseEntity<String> resp, String accessToken, String refreshToken) {

        try {
            List<String> newAccessToken = resp.getHeaders().get(Constants.ACCESS_TOKEN);

            if (newAccessToken != null && !newAccessToken.isEmpty() && newAccessToken.get(0) != null) {

                if (!newAccessToken.get(0).equals(accessToken)) {

                    try {
                        Mono<Void> validatedTokenMono = validateAuthenticationToken(exchange, chain, newAccessToken.get(0));
                        return addCookiesInResponse(exchange, validatedTokenMono, newAccessToken.get(0), refreshToken);
                    } catch (JWTVerificationException ex) {

                        throw new RuntimeException(ex.getMessage());
                    }
                } else {
                    throw new RuntimeException("Access token is same as expired one");
                }
            } else {
                throw new RuntimeException("No access token passed in header when refresh endpoint was hit.");
            }

        } catch (Exception e) {
            return Utility.onError(exchange, e.getMessage());
        }
    }

    private Mono<Void> addCookiesInResponse(ServerWebExchange exchange, Mono<Void> mono, String accessToken, String refreshToken) {

        return mono.then(Mono.fromRunnable(() -> {

            // Modify the response here
            ServerHttpResponse response = exchange.getResponse();

            ResponseCookie accessTokenCookie = ResponseCookie.from(Constants.ACCESS_TOKEN, accessToken)
                    .httpOnly(true)
                    .path("/")
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from(Constants.REFRESH_TOKEN, refreshToken)
                    .httpOnly(true)
                    .path("/")
                    .build();

            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);
        }));
    }

    private Mono<ResponseEntity<String>> sendRequestToUserAuthenticationServiceForNewAccessToken(String accessToken, String refreshToken) {

        WebClient webClient = webClientBuilder.baseUrl(getServiceUri("mym-user-authentication-service")).build();

        return webClient.get()
                .uri("/mym/api/users/token/refresh/cookie")
                .cookie(Constants.ACCESS_TOKEN, accessToken)
                .cookie(Constants.REFRESH_TOKEN, refreshToken)
                .exchangeToMono(response -> response.toEntity(String.class));


    }

    private Mono<Void> validateAuthenticationToken(ServerWebExchange exchange, GatewayFilterChain chain, String accessToken) throws JWTVerificationException {

        Algorithm algorithm = Algorithm.HMAC256(tokenSecret.getBytes());
        DecodedJWT decodedJWT = Utility.isTokenValid(accessToken, algorithm);

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

        ServerWebExchange.Builder request;
        try {
            request = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header(Constants.USER_INFO_HEADER_NAME, new ObjectMapper().writeValueAsString(userInfo))
                            .header(HttpHeaders.AUTHORIZATION, BEARER + " " + accessToken)
                            .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return chain.filter(request.build());
    }

    private String getServiceUri(String serviceName) {

        return discoveryClient.getInstances(serviceName)
                .stream()
                .findFirst()
                .map(si -> si.getUri().toString())
                .orElseThrow(() -> new RuntimeException(serviceName + " service instance not found"));
    }
}
