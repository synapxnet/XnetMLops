package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 知识库文档实体类
 */
@Data
public class KBDocument {
    private Long id;
    private String uid;
    private Long kb_id;

    // 文档信息
    private String name;
    private String original_name;
    private String type;
    private String mime_type;
    private String file_path;
    private Long file_size;
    private String file_hash;

    // 处理状态
    private String status;
    private Integer process_progress;
    private String error_message;

    // 统计信息
    private Integer word_count;
    private Integer char_count;
    private Integer chunk_count;
    private Integer token_count;
    private Integer page_count;

    // 元数据
    private String metadata;
    private String source_url;
    private String source_type;

    // 自定义配置
    private Integer custom_chunk_size;
    private Integer custom_chunk_overlap;

    // 操作信息
    private String uploaded_by;
    private Date created_at;
    private Date updated_at;
    private Date indexed_at;
}
