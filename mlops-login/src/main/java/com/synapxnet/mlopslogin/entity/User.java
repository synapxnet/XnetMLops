package com.synapxnet.mlopslogin.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class User {
    private int id;
    private String uid;
    private String phone;
    private String realName;
    private String roles;
    private String username;
    private String userId;
    private String permission;
    private LocalDateTime roles_failure_time;

    public void setRoles(String roles) { this.roles = roles; }
    public void setPermission(String permission) { this.permission = permission; }

    public List<String> getRoles() {
        return parseJsonToList(this.roles);
    }

    public List<String> getPermission() {
        return parseJsonToList(this.permission);
    }

    // 手动反序列化工具方法
    private List<String> parseJsonToList(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

}
