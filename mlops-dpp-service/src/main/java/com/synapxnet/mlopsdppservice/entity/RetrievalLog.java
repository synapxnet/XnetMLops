package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 检索日志实体类
 */
@Data
public class RetrievalLog {
    private Long id;
    private Long kb_id;

    // 查询信息
    private String query;
    private Integer query_tokens;

    // 检索配置
    private String retrieval_method;
    private Integer top_k;
    private BigDecimal score_threshold;
    private Boolean rerank_enabled;

    // 检索结果
    private Integer result_count;
    private String results;

    // 性能指标
    private Integer embedding_latency_ms;
    private Integer retrieval_latency_ms;
    private Integer rerank_latency_ms;
    private Integer total_latency_ms;

    // 来源信息
    private String source;
    private String session_id;
    private String user_id;
    private String tenant_uid;

    // 反馈
    private Integer feedback_score;
    private String feedback_comment;

    private Date created_at;
}
