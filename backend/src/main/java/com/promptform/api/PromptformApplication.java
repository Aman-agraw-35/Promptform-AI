package com.promptform.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PromptformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromptformApplication.class, args);
    }
}
