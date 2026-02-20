package com.sumu.japdemo.entity.dto;

import lombok.Data;

@Data
public class SignalingMessage {
    private String type;
    private Object payload;
}
