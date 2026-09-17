/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly forbidden to copy, distribute, or use without explicit authorization.
 * 模型制品登记接口。 / Model artifact registry API.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-16 | Version: 1.0.0 | Security Level: INTERNAL
 */
package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.entity.ModelArtifact;
import com.synapxnet.mlopsmtpservice.mapper.ModelArtifactMapper;
import com.synapxnet.mlopsmtpservice.service.HdfsService;
import org.apache.hadoop.fs.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 提供租户范围内的模型制品创建和查询。 / Provides tenant-scoped model artifact creation and reads. */
@RestController
@RequestMapping("/api/mtp/model-artifacts")
public class ModelArtifactController {
    private final ModelArtifactMapper mapper;
    private final HdfsService hdfsService;

    /** 注入登记映射和 HDFS 处理器。 / Inject the registry mapper and HDFS processor. */
    public ModelArtifactController(ModelArtifactMapper mapper, HdfsService hdfsService) { this.mapper = mapper; this.hdfsService = hdfsService; }

    /** 查询当前租户的模型制品。 / List model artifacts for the current tenant. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestHeader("X-Tenant-Id") String tenantUid,
                                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "用户身份不能为空"));
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", mapper.findByTenant(tenantUid)));
    }

    /** 登记模型并在提供临时文件时转存 HDFS。 / Register a model and move an optional temporary file to HDFS. */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> create(@RequestBody ModelArtifact request,
                                                       @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                                       @RequestHeader(value = "X-Tenant-Uid", required = false) String tenantUidHeader,
                                                       @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        String trustedTenant = (tenantHeader != null && !tenantHeader.isBlank()) ? tenantHeader : tenantUidHeader;
        if (trustedTenant == null || trustedTenant.isBlank() || userHeader == null || userHeader.isBlank()
                || request.getOutputName() == null || request.getOutputName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "租户和输出名称不能为空"));
        }
        request.setTenantUid(trustedTenant);
        request.setUserId(userHeader);
        request.setOutputName(request.getOutputName().trim());
        if (mapper.countByName(request.getTenantUid(), request.getOutputName()) > 0) {
            return ResponseEntity.ok(Map.of("code", 409, "message", "当前租户下输出名称已存在"));
        }
        request.setUid("MO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        Date now = new Date(); request.setCreatedAt(now); request.setUpdatedAt(now);
        String temporary = request.getArtifactPath();
        if (temporary != null && !temporary.isBlank()) {
            String sourceName = new Path(temporary).getName().replaceFirst("\\.temp$", "");
            String target = "/models/" + request.getTenantUid() + "/" + request.getUid() + "/" + sourceName;
            try { hdfsService.moveAndProcessFile(temporary, target); request.setArtifactPath(target); }
            catch (Exception ex) { return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "模型文件转存失败")); }
        }
        mapper.insert(request);
        return ResponseEntity.ok(Map.of("code", 0, "message", "模型制品登记成功", "data", request));
    }
}
