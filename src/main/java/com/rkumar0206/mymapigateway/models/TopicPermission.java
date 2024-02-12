package com.rkumar0206.mymapigateway.models;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TopicPermission {

    private String exchange;
    private String write;
    private String read;
}
