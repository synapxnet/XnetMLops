package com.synapxnet.mlopsmepservice;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(
        basePackages = {
                "com.synapxnet.mlopsmepservice.mapper",
                "com.synapxnet.mlopsmepservice.agent"
        },
        annotationClass = Mapper.class)
public class MlopsMepServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlopsMepServiceApplication.class, args);
    }
}
