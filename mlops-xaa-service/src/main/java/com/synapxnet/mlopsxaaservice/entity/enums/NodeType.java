package com.synapxnet.mlopsxaaservice.entity.enums;

/**
 * 工作流节点类型枚举
 * 参考Dify NodeType设计，适配XnetMLops的DPP/MTP/MEP模块
 */
public enum NodeType {
    // 基础节点
    START("start", "开始节点"),
    END("end", "结束节点"),

    // DPP 数据处理相关节点
    DPP_DATASET("dpp-dataset", "DPP数据集节点"),
    DPP_FEATURE("dpp-feature", "DPP特征工程节点"),
    DPP_TASK("dpp-task", "DPP数据任务节点"),

    // MTP 模型训练相关节点
    MTP_ALGORITHM("mtp-algorithm", "MTP算法节点"),
    MTP_TRAIN("mtp-train", "MTP训练任务节点"),
    MTP_OUTPUT("mtp-output", "MTP模型输出节点"),

    // MEP 模型部署相关节点
    MEP_DEPLOY("mep-deploy", "MEP部署节点"),
    MEP_SERVICE("mep-service", "MEP服务节点"),

    // 流程控制节点
    IF_ELSE("if-else", "条件分支节点"),
    LOOP("loop", "循环节点"),
    PARALLEL("parallel", "并行节点"),

    // 工具节点
    HTTP_REQUEST("http-request", "HTTP请求节点"),
    CODE("code", "代码执行节点"),
    VARIABLE_ASSIGNER("variable-assigner", "变量赋值节点"),
    TEMPLATE_TRANSFORM("template-transform", "模板转换节点"),

    // 等待节点
    WAIT("wait", "等待节点"),
    HUMAN_INPUT("human-input", "人工输入节点");

    private final String value;
    private final String label;

    NodeType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static NodeType fromValue(String value) {
        for (NodeType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + value);
    }
}
