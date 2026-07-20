package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 文档分块实体类
 */
@Data
public class KBChunk {
    private Long id;
    private String uid;
    private Long doc_id;
    private Long kb_id;

    // 内容
    private String content;
    private String content_hash;

    // 位置信息
    private Integer position;
    private Integer start_index;
    private Integer end_index;
    private Integer page_number;

    // 向量信息
    private String embedding_id;
    private Integer token_count;
    private Integer char_count;

    // 层级结构
    private Long parent_chunk_id;
    private Integer chunk_level;

    // 元数据
    private String metadata;
    private String keywords;
    private String summary;

    private Date created_at;
}
