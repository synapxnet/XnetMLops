package com.synapxnet.mlopslogin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.synapxnet.mlopslogin.mapper")
public class MlopsLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlopsLoginApplication.class, args);
    }
    

}
