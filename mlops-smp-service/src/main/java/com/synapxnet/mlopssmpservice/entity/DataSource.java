package com.synapxnet.mlopssmpservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Date;

@Data
public class DataSource {
    private Long id;
    private String uid;
    private String name;
    private String type;           // mysql, postgresql, oracle, sqlserver, hive, clickhouse
    private String host;
    private Integer port;
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String defaultDatabase;   // 默认数据库（用于连接）
    private String allowedDatabases;  // 允许访问的数据库列表，用逗号分隔
    private String description;
    private Boolean enabled;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
}
