package com.synapxnet.mlopssmpservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mlops_smp_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=never",
    "jenkins.url=http://127.0.0.1:8080",
    "jenkins.username=test-user",
    "jenkins.api-token=test-only-token",
    "hdfs.path=hdfs://127.0.0.1:8020",
    "hdfs.user=test-user"
})
class MlopsSmpServiceApplicationTests {

    /**
     * 验证模型服务管理模块在隔离的测试依赖下能够完整加载 Spring 应用上下文。
     */
    @Test
    void contextLoads() {
    }

}
