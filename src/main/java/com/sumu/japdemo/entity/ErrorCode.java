package com.sumu.japdemo.entity;

import lombok.Data;
import lombok.Getter;

@Getter

public enum ErrorCode {
    //枚举类
    SUCCESS(200, "成功"),
    FAIL(500, "失败");
    private int code;
    private String message;
    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
