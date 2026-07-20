package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class HarborRepository {
    private Integer id;
    private String uid;
    private String name;
    private String url;
    private String username;
    private String password;
    private String created_by;
    private String updated_by;
    private Timestamp created_at;
    private Timestamp updated_at;
}
