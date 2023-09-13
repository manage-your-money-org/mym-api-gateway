package com.rkumar0206.mymapigateway.feignClients;

import com.rkumar0206.mymapigateway.models.CustomResponse;
import com.rkumar0206.mymapigateway.models.UserAccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "MYM-USER-AUTHENTICATION-SERVICE"
)
public interface UserAuthenticationFeignClient {

    @GetMapping("/mym/api/users/details")
    ResponseEntity<CustomResponse<UserAccountResponse>> getUserDetails(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    );
}
