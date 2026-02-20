package com.sumu.japdemo.controller;

import com.sumu.japdemo.entity.Msg;
import com.sumu.japdemo.utils.JapJsonUtil;
import com.sumu.japdemo.utils.OkHttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/hello")
public class HelloController {
    @Autowired
    private JapJsonUtil japJsonUtil;
    @Autowired
    private OkHttpUtil okHttpUtil;
    @GetMapping("/gethello")
    public String hello() {
        System.out.println("hello");
        List<String> list= new ArrayList<>();
        list.add("hello");
        list.add("gethello");
        String s= japJsonUtil.ToJson(list);
        return s;
    }
    @GetMapping("/gethttp")
    public Msg gethttp() {
        try {
            String s = okHttpUtil.get("http://localhost:8080/hello/gethello");
            return Msg.success(s);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
