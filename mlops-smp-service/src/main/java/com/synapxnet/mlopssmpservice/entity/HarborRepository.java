package com.synapxnet.mlopssmpservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.sql.Timestamp;

@Data
public class HarborRepository {
    private Integer id;
    private String uid;
    private String name;
    private String url;
    private String username;

    // 密码只允许写入（接收前端传入），不允许读取（不返回给前端）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String created_by;
    private String updated_by;
    private Timestamp created_at;
    private Timestamp updated_at;
}
