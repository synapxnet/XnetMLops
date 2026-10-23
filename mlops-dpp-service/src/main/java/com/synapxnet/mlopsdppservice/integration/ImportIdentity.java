package com.synapxnet.mlopsdppservice.integration;

/** 保存由可信网关注入的数据集导入组织身份。 */
public record ImportIdentity(
        String userId,
        String tenantUid,
        String teamUid,
        String deptUid,
        String teamName,
        int organizationLevel
) {
}
