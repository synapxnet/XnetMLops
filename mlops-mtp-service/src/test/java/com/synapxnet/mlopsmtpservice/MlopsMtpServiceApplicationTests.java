package com.synapxnet.mlopsmtpservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mlops_mtp_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
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
class MlopsMtpServiceApplicationTests {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 验证模型训练服务在隔离的测试依赖下能够完整加载 Spring 应用上下文。
     */
    @Test
    void contextLoads() {
    }

}
