/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. No copying, distribution or use without authorization.
 * 原生查询字段映射回归 / Native query result mapping regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopssmpservice.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopssmpservice.entity.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativeResultMappingTest {
    /** 使用真实Mapper声明覆盖每个返回实体的查询。 Cover each entity-returning query from the actual mapper declarations. */
    static Stream<Object[]> mappings() {
        return Stream.of(
                new Object[]{AlgorithmRepositoryMapper.class, AlgorithmRepository.class},
                new Object[]{FeatureOperatorMapper.class, FeatureOperator.class},
                new Object[]{DockerFileMapper.class, DockerFile.class},
                new Object[]{HarborRepositoryMapper.class, HarborRepository.class},
                new Object[]{JenkinsMasterMapper.class, JenkinsMaster.class},
                new Object[]{JenkinsNodeMapper.class, JenkinsNode.class});
    }

    /** 通过MyBatis真正读取带下划线列名的结果集，避免只检查注解存在。 Read snake-case result sets through MyBatis rather than only checking annotation presence. */
    @ParameterizedTest
    @MethodSource("mappings")
    void preservesNativeColumnsForEveryRead(Class<?> mapper, Class<?> entity) throws Exception {
        var configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(mapper);
        List<Field> fields = Arrays.stream(entity.getDeclaredFields())
                .filter(field -> field.getName().contains("_") && !field.getName().equals("admin_password"))
                .toList();
        for (var method : mapper.getDeclaredMethods()) {
            if (method.getAnnotation(org.apache.ibatis.annotations.Select.class) == null || method.getReturnType() == int.class) continue;
            var mapped = configuration.getMappedStatement(mapper.getName() + "." + method.getName());
            var metadata = new RowSetMetaDataImpl();
            metadata.setColumnCount(fields.size());
            var rowset = RowSetProvider.newFactory().createCachedRowSet();
            for (int index = 0; index < fields.size(); index++) {
                var field = fields.get(index);
                metadata.setColumnName(index + 1, field.getName());
                metadata.setColumnLabel(index + 1, field.getName());
                metadata.setColumnType(index + 1, field.getType() == byte[].class ? Types.VARBINARY : field.getType() == java.util.Date.class || field.getType() == Timestamp.class ? Types.TIMESTAMP : field.getType() == Integer.class ? Types.INTEGER : Types.VARCHAR);
            }
            rowset.setMetaData(metadata);
            rowset.moveToInsertRow();
            for (int index = 0; index < fields.size(); index++) rowset.updateObject(index + 1, sample(fields.get(index)));
            rowset.insertRow();
            rowset.moveToCurrentRow();
            rowset.beforeFirst();
            var statement = mock(Statement.class);
            var connection = mock(Connection.class);
            // JDK缓存结果集缺少isClosed实现，其余读取交给真实结果集。 Supply only the missing isClosed operation and delegate all reads to the real rowset.
            var readable = (ResultSet) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class}, (proxy, operation, arguments) -> {
                if (operation.getName().equals("isClosed")) return false;
                try { return operation.invoke(rowset, arguments); }
                catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
            });
            when(statement.getResultSet()).thenReturn(readable);
            when(statement.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(mock(DatabaseMetaData.class));
            var handler = new DefaultResultSetHandler(mock(Executor.class), mapped, mock(ParameterHandler.class), null, mapped.getBoundSql(new HashMap<>()), RowBounds.DEFAULT);
            Object result = handler.handleResultSets(statement).get(0);
            for (Field field : fields) {
                field.setAccessible(true);
                Object expected = sample(field);
                Object actual = field.get(result);
                if (expected instanceof byte[] bytes) assertArrayEquals(bytes, (byte[]) actual, method.getName() + ":" + field.getName());
                else if (expected instanceof Timestamp timestamp) assertEquals(timestamp.getTime(), ((java.util.Date) actual).getTime(), method.getName() + ":" + field.getName());
                else if (field.getType().isEnum()) assertEquals(expected, actual.toString().toUpperCase(Locale.ROOT));
                else assertEquals(expected, actual, method.getName() + ":" + field.getName());
            }
        }
    }

    /** 创建无真实凭据的非空列值。 Create nonempty column values without real credentials. */
    private static Object sample(Field field) {
        if (field.getType() == byte[].class) return new byte[]{1, 2, 3};
        if (field.getType() == java.util.Date.class || field.getType() == Timestamp.class) return Timestamp.valueOf("2026-10-23 12:00:00");
        if (field.getType() == Integer.class) return 7;
        if (field.getType().isEnum()) return "PUSHED";
        return "local-" + field.getName();
    }

    /** 恢复后端字段映射不能使凭据重新出现在JSON输出。 Restored backend fields must not disclose credentials in JSON responses. */
    @Test
    void credentialsRemainWriteOnly() throws Exception {
        var mapper = new ObjectMapper();
        var algorithm = new AlgorithmRepository(); algorithm.setEncrypted_token(new byte[]{1});
        var operator = new FeatureOperator(); operator.setEncrypted_token(new byte[]{1});
        var master = new JenkinsMaster(); master.setEncrypted_password("encrypted"); master.setEncrypted_admin_password("encrypted"); master.setCredentials_config("sensitive"); master.setInitial_password("sensitive");
        assertFalse(mapper.valueToTree(algorithm).has("encrypted_token"));
        assertFalse(mapper.valueToTree(operator).has("encrypted_token"));
        var serialized = mapper.valueToTree(master);
        for (String field : List.of("encrypted_password", "encrypted_admin_password", "credentials_config", "initial_password")) assertFalse(serialized.has(field));
    }
}
