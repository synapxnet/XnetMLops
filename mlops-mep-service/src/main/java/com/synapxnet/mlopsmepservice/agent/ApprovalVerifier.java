/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.mlopsmepservice.agent.dto.MepAgentDtos;

/**
 * 定义高风险回滚在执行前必须通过的 OpenXnet 审批验证端口。
 */
public interface ApprovalVerifier {

    /**
     * 校验审批状态、范围、摘要、资源版本和职责分离。
     *
     * @param approvalId 审批引用
     * @param arguments 回滚领域参数
     * @param requestDigest 规范化请求摘要
     * @param expectedResourceVersion 预期资源版本
     * @param context 已验证调用上下文
     * @return 已验证审批决策
     */
    ApprovalDecision verify(
            String approvalId,
            MepAgentDtos.RollbackArguments arguments,
            String requestDigest,
            String expectedResourceVersion,
            AgentContract.RequestContext context);

    /** 表示经过服务端验证的审批决策。 */
    record ApprovalDecision(String approvalId, String approverId, String executorId) {
    }
}
