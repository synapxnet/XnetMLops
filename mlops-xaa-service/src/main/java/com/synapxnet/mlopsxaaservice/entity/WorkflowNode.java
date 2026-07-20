package com.synapxnet.mlopsxaaservice.entity;

import lombok.Data;
import java.util.Date;

/**
 * 工作流节点实体类
 * 支持的节点类型参考Dify: start, end, dpp-task, mtp-task, mep-task, if-else, loop等
 */
@Data
public class WorkflowNode {
    private Long id;
    private String uid;

    /**
     * 所属工作流ID
     */
    private Long workflowId;

    /**
     * 节点类型
     * start: 开始节点
     * end: 结束节点
     * dpp-task: DPP数据处理任务
     * mtp-task: MTP模型训练任务
     * mep-task: MEP模型部署任务
     * if-else: 条件分支
     * loop: 循环节点
     * http-request: HTTP请求
     * code: 代码执行
     * variable-assigner: 变量赋值
     */
    private String nodeType;

    /**
     * 节点标题
     */
    private String title;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 节点配置JSON
     */
    private String configJson;

    /**
     * 节点在画布上的位置X
     */
    private Double positionX;

    /**
     * 节点在画布上的位置Y
     */
    private Double positionY;

    /**
     * 节点宽度
     */
    private Double width;

    /**
     * 节点高度
     */
    private Double height;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    private Date createdAt;
    private Date updatedAt;
}
