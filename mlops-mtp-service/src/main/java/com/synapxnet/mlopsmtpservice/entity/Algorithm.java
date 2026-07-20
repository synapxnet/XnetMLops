package com.synapxnet.mlopsmtpservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import org.springframework.data.annotation.Transient;
import java.util.Date;

@Data
public class Algorithm {
    private Long id;
    private String uid;
    private String userId;
    private String algorithm_name;
    private String version;
    private String zone;
    private String zone_label;
    private boolean encryption;
    private String subdata_area;
    private String bucket_name;
    private String bucket_identifier;
    private String team_uid;
    private String team_name;
    private String description;
    private String tenant_uid;
    private String dept_uid;
    private int level;
    @JsonProperty("is_CAS") // 明确指定JSON属性名
    private Boolean isCAS;
    private Date created_at;
    private Date updated_at;
    private String cloud_algorithm_id;

    @Transient //临时存储路径
    private String tempFilePath;
}
