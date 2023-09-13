package com.rkumar0206.mymapigateway.models;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAccountResponse {

    private String name;
    private String emailId;
    private String uid;
    private boolean isAccountVerified;
}
