package com.synapxnet.mlopssmpservice.entity;

import lombok.Data;
import org.apache.ibatis.type.EnumTypeHandler;

import java.sql.Timestamp;

@Data
public class DockerFile {
    private Integer id;
    private String uid;
    private String name;
    private String content;
    private String tags;
    private PushStatus push_status;
    private String harbor_uid;
    private String push_history;
    private String created_by;
    private String updated_by;
    private Timestamp created_at;
    private Timestamp updated_at;

    public enum PushStatus {
        PENDING, PUSHING, PUSHED, FAILED;

        // 统一转换方法
        public static PushStatus fromString(String value) {
            return value == null ? null : PushStatus.valueOf(value.toUpperCase());
        }

        // 统一输出格式
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}