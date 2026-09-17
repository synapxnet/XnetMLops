/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 模型证据只读接口。 / Read-only API for recorded model evidence.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-13
 * Version: 1.0.0 | Security Level: INTERNAL | Maintainer: maoyo
 * Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.evidence.ModelEvidenceService;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingWriteAuthorizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 复用登录与租户成员校验，无新增写入口。 / Reuse authenticated tenant membership with no new write endpoint. */
@RestController
@RequestMapping("/api/mtp/model-evidence")
public class ModelEvidenceController {
    private final ModelEvidenceService evidenceService;
    private final RecommendationTrainingWriteAuthorizer authorizer;

    /** 注入证据服务和既有身份校验器。 / Inject evidence and the existing identity verifier. */
    public ModelEvidenceController(ModelEvidenceService evidenceService,
                                   RecommendationTrainingWriteAuthorizer authorizer) {
        this.evidenceService = evidenceService;
        this.authorizer = authorizer;
    }

    /** 先验证真实成员关系，再查询该租户记录。 / Verify actual membership before querying tenant records. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> read(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestHeader("X-User-Id") String userId) {
        authorizer.authorize(authorization, userId, tenantUid);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success",
                "data", evidenceService.read(tenantUid)));
    }
}
