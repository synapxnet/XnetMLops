package com.synapxnet.mlopsdppservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mlops_dpp_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "jenkins.url=http://127.0.0.1:8080",
    "jenkins.username=test-user",
    "jenkins.api-token=test-only-token",
    "hdfs.path=hdfs://127.0.0.1:8020",
    "hdfs.user=test-user"
})
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
class MlopsDppServiceApplicationTests {

    /**
     * 验证数据预处理服务在隔离的测试依赖下能够完整加载 Spring 应用上下文。
     */
    @Test
    void contextLoads() {
    }

}
