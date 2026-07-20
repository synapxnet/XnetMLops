package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 节点执行记录实体类
 * 记录工作流中每个节点的执行状态
 */
@Data
public class NodeExecution {
    private Long id;
    private String uid;

    /**
     * 所属工作流执行ID
     */
    private Long executionId;

    /**
     * 所属工作流ID
     */
    private Long workflowId;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点标题
     */
    private String nodeTitle;

    /**
     * 执行状态
     * pending: 等待中
     * running: 运行中
     * succeeded: 成功
     * failed: 失败
     * skipped: 跳过
     * stopped: 已停止
     */
    private String status;

    /**
     * 输入参数JSON
     */
    private String inputsJson;

    /**
     * 输出结果JSON
     */
    private String outputsJson;

    /**
     * 执行元数据JSON (如token消耗、外部任务ID等)
     */
    private String metadataJson;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private Date startedAt;

    /**
     * 结束时间
     */
    private Date finishedAt;

    /**
     * 执行耗时(毫秒)
     */
    private Long elapsedTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    private Date createdAt;
    private Date updatedAt;
}
