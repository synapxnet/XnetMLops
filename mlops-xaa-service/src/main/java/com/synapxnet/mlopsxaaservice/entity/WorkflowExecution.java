package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 工作流执行记录实体类
 * 记录工作流的每次运行
 */
@Data
public class WorkflowExecution {
    private Long id;
    private String uid;

    /**
     * 所属工作流ID
     */
    private Long workflowId;

    /**
     * 执行状态
     * scheduled: 已调度
     * running: 运行中
     * succeeded: 成功
     * failed: 失败
     * stopped: 已停止
     * paused: 暂停中
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
     * 触发者ID
     */
    private String triggeredBy;

    /**
     * 触发类型: manual, scheduled, api
     */
    private String triggerType;

    private Date createdAt;
    private Date updatedAt;
}
