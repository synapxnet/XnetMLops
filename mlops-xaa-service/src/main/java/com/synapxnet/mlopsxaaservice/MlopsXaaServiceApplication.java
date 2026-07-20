package com.synapxnet.mlopsxaaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MlopsXaaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlopsXaaServiceApplication.class, args);
    }

}
