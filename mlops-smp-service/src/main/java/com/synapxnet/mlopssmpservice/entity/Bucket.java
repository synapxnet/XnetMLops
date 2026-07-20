package com.synapxnet.mlopssmpservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.*;

@Data
public class Bucket {

    private Long id;
    private String uid;
    private String name;
    private String identifier;
    private String type; // "public" or "tenant"
    private String tenant_uid;
    private String dept_uid;
    private String team_uid;
    private Double current_size;
    private Double max_size;
    private String status; // "active" or "disabled"
    private Date created_at;
    private Date updated_at;

    // 新增字段 - 敏感信息只写不读
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String access_key;
    private String authorized_tenants; // 授权租户列表

    // 关联信息（非数据库字段）
    private String tenant_name;
    private String dept_name;
    private String team_name;

}
