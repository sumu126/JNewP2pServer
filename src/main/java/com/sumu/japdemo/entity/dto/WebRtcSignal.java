package com.sumu.japdemo.entity.dto;

import lombok.Data;

@Data
public class WebRtcSignal {
    private String targetUserId;
    private String fromUserId;
    private Object signal;
}
