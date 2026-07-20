package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 嵌入模型配置实体类
 */
@Data
public class EmbeddingModel {
    private Long id;
    private String name;
    private String provider;
    private String model_name;

    // 模型参数
    private Integer dimension;
    private Integer max_tokens;
    private Integer batch_size;

    // API配置
    private String api_endpoint;
    private String api_key_encrypted;
    private String extra_config;

    // 状态
    private Boolean is_default;
    private String status;
    private String tenant_uid;

    private Date created_at;
    private Date updated_at;
}
