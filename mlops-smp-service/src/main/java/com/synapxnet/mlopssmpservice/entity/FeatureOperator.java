package com.synapxnet.mlopssmpservice.entity;

import lombok.Data;
import java.util.Date;

@Data
public class FeatureOperator {
    private Long id;
    private String uid;
    private String url;
    private byte[] encrypted_token;
    private String operator_name;
    private String operator_code;
    private String operator_version;
    private String description;

    private String tenant_uid;
    private String dept_uid;
    private String team_uid;

    private String created_by;
    private String updated_by;

    private Date created_at;
    private Date updated_at;

    // 授权列表
    private String authorized_tenants;

    // 关联信息（非数据库字段）
    private String tenant_name;
    private String dept_name;
    private String team_name;
}
