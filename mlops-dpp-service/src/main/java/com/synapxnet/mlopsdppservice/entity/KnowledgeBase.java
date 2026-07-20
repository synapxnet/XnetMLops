package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 知识库实体类
 */
@Data
public class KnowledgeBase {
    private Long id;
    private String uid;
    private String name;
    private String description;
    private String icon;

    // 嵌入模型配置
    private Long embedding_model_id;
    private String embedding_provider;
    private String embedding_model;
    private Integer embedding_dimension;

    // 向量数据库配置
    private String vector_db_type;
    private String vector_collection;
    private String vector_index_type;

    // 分块配置
    private String chunk_strategy;
    private Integer chunk_size;
    private Integer chunk_overlap;
    private String chunk_separator;

    // 检索配置
    private String retrieval_method;
    private Integer top_k;
    private BigDecimal score_threshold;
    private Boolean rerank_enabled;
    private String rerank_model;
    private Integer rerank_top_k;

    // 统计信息
    private String status;
    private Integer doc_count;
    private Integer chunk_count;
    private Long total_tokens;
    private Long total_size_bytes;

    // 租户与权限
    private String tenant_uid;
    private String creator_id;
    private String visibility;

    private Date created_at;
    private Date updated_at;
}
