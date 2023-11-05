package com.rkumar0206.mymapigateway.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateExchange {

    private String type;
    private boolean auto_delete;
    private boolean durable;
    private boolean internal;
}
