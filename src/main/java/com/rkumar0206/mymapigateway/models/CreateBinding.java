package com.rkumar0206.mymapigateway.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBinding {

    private String routing_key;
    //private Map<String, String> arguments;
}
