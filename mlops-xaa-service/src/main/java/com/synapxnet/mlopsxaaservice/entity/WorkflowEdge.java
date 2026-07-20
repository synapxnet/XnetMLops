package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 工作流边(连接线)实体类
 * 表示节点之间的连接关系
 */
@Data
public class WorkflowEdge {
    private Long id;
    private String uid;

    /**
     * 所属工作流ID
     */
    private Long workflowId;

    /**
     * 源节点ID
     */
    private Long sourceNodeId;

    /**
     * 源节点的输出端口
     */
    private String sourceHandle;

    /**
     * 目标节点ID
     */
    private Long targetNodeId;

    /**
     * 目标节点的输入端口
     */
    private String targetHandle;

    /**
     * 边类型: default, success, fail
     */
    private String edgeType;

    /**
     * 条件表达式 (用于条件分支)
     */
    private String conditionJson;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    private Date createdAt;
    private Date updatedAt;
}
