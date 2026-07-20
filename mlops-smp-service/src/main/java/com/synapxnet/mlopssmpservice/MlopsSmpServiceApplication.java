package com.synapxnet.mlopssmpservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MlopsSmpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlopsSmpServiceApplication.class, args);
    }

}
