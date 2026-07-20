package com.synapxnet.mlopsxaaservice.entity.enums;

/**
 * 工作流/节点执行状态枚举
 * 参考Dify WorkflowExecutionStatus设计
 */
public enum ExecutionStatus {
    SCHEDULED("scheduled", "已调度"),
    PENDING("pending", "等待中"),
    RUNNING("running", "运行中"),
    SUCCEEDED("succeeded", "成功"),
    FAILED("failed", "失败"),
    STOPPED("stopped", "已停止"),
    PAUSED("paused", "暂停中"),
    SKIPPED("skipped", "已跳过");

    private final String value;
    private final String label;

    ExecutionStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEnded() {
        return this == SUCCEEDED || this == FAILED || this == STOPPED || this == SKIPPED;
    }

    public static ExecutionStatus fromValue(String value) {
        for (ExecutionStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown execution status: " + value);
    }
}
