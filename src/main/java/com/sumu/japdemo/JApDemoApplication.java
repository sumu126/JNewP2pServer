package com.sumu.japdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.sumu.japdemo.mapper")
@EnableScheduling
public class JApDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JApDemoApplication.class, args);
    }

}
