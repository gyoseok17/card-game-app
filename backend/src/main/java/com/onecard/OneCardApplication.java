package com.onecard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OneCardApplication {
    public static void main(String[] args) {
        SpringApplication.run(OneCardApplication.class, args);
    }
}
