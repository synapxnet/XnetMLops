package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingRequest;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingRun;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingService;
import com.synapxnet.mlopsmtpservice.recommendation.RecommendationTrainingWriteAuthorizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mtp/recommendation-training")
public class RecommendationTrainingController {

    private final RecommendationTrainingService trainingService;
    private final RecommendationTrainingWriteAuthorizer writeAuthorizer;

    /** 注入推荐模型训练服务。 */
    public RecommendationTrainingController(
            RecommendationTrainingService trainingService,
            RecommendationTrainingWriteAuthorizer writeAuthorizer
    ) {
        this.trainingService = trainingService;
        this.writeAuthorizer = writeAuthorizer;
    }

    /** 按租户查询最近的推荐模型训练运行。 */
    @GetMapping("/runs")
    public ResponseEntity<Map<String, Object>> listRuns(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestHeader("X-User-Id") String userId
    ) {
        writeAuthorizer.authorize(authorization, userId, tenantUid);
        return success(trainingService.listRuns(tenantUid));
    }

    /** 使用审批号和幂等键同步执行一次受控 CPU DCN 训练。 */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runTraining(
            @RequestBody RecommendationTrainingRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Approval-Id") String approvalId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        writeAuthorizer.authorize(authorization, userId, tenantUid);
        RecommendationTrainingRun run = trainingService.runTraining(
                request,
                tenantUid,
                userId,
                approvalId,
                idempotencyKey
        );
        return success(run);
    }

    /** 构造与现有 MTP 接口一致的成功响应。 */
    private ResponseEntity<Map<String, Object>> success(Object data) {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", data,
                "error", "null"
        ));
    }
}
