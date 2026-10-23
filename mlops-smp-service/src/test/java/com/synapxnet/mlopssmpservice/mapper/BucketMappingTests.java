/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Copying, distribution or use requires explicit authorization.
 * 存储桶真实SQL字段映射回归 / Bucket SQL field mapping regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.Bucket;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BucketMappingTests {
    /** 通过实际Mapper SQL回读容量、权限范围与凭据字段。 Read actual mapper SQL to preserve capacity, scope and credentials. */
    @Test void allBucketQueriesPreserveSnakeCaseColumns() throws Exception {
        UnpooledDataSource ds = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:bucket_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = ds.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE xnet_mlops_sys_tenant(uid VARCHAR,tenant_name VARCHAR)");
            statement.execute("CREATE TABLE xnet_mlops_sys_department(uid VARCHAR,dept_name VARCHAR)");
            statement.execute("CREATE TABLE xnet_mlops_sys_team(uid VARCHAR,team_name VARCHAR)");
            statement.execute("CREATE TABLE xnet_mlops_sys_bucket(id BIGINT,uid VARCHAR,name VARCHAR,identifier VARCHAR,type VARCHAR,tenant_uid VARCHAR,dept_uid VARCHAR,team_uid VARCHAR,current_size DOUBLE,max_size DOUBLE,access_key VARCHAR,authorized_tenants VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP)");
            statement.execute("INSERT INTO xnet_mlops_sys_tenant VALUES('tenant-test','Test tenant')");
            statement.execute("INSERT INTO xnet_mlops_sys_department VALUES('dept-test','Test department')");
            statement.execute("INSERT INTO xnet_mlops_sys_team VALUES('team-test','Test team')");
            statement.execute("INSERT INTO xnet_mlops_sys_bucket VALUES(1,'bucket-test','Test bucket','test','tenant','tenant-test','dept-test','team-test',12.5,80,'fixture-secret','tenant-test',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        }
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(BucketMapper.class);
        try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession()) {
            BucketMapper mapper = session.getMapper(BucketMapper.class);
            assertActualFields(mapper.selectBucketsWithOrgInfo().get(0));
            assertActualFields(mapper.findById(1L).orElseThrow());
            assertActualFields(mapper.selectBucketById(1L));
        }
    }

    /** 校验实际持久化数据，不以非空占位代替。 Assert the persisted values instead of non-null placeholders. */
    private static void assertActualFields(Bucket bucket) {
        assertEquals(12.5, bucket.getCurrent_size());
        assertEquals(80.0, bucket.getMax_size());
        assertEquals("tenant-test", bucket.getTenant_uid());
        assertEquals("dept-test", bucket.getDept_uid());
        assertEquals("team-test", bucket.getTeam_uid());
        assertEquals("Test tenant", bucket.getTenant_name());
        assertEquals("Test department", bucket.getDept_name());
        assertEquals("Test team", bucket.getTeam_name());
        assertEquals("fixture-secret", bucket.getAccess_key());
        assertEquals("tenant-test", bucket.getAuthorized_tenants());
        assertNotNull(bucket.getCreated_at());
    }
}
