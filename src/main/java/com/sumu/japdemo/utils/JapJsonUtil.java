package com.sumu.japdemo.utils;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

@Component
public class JapJsonUtil<T> {
    public String ToJson(T obj) {
        return JSON.toJSONString(obj);
    }
    public T FromJson(String json,Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
}
