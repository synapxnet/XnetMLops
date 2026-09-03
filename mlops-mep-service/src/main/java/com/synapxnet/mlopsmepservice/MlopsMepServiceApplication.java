package com.synapxnet.mlopsmepservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.synapxnet.mlopsmepservice.mapper")
public class MlopsMepServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlopsMepServiceApplication.class, args);
    }
}
