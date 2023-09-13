package com.rkumar0206.mymapigateway.models;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CustomResponse<T> {

    private int code;
    private String message;
    private T body;
}
