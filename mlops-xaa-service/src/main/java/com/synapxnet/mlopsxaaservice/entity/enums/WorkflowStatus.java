package com.synapxnet.mlopsxaaservice.entity.enums;

/**
 * 工作流状态枚举
 */
public enum WorkflowStatus {
    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    ARCHIVED("archived", "已归档");

    private final String value;
    private final String label;

    WorkflowStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static WorkflowStatus fromValue(String value) {
        for (WorkflowStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown workflow status: " + value);
    }
}
