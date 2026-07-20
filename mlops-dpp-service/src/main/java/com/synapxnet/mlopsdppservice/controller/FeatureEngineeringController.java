package com.synapxnet.mlopsdppservice.controller;

import com.synapxnet.mlopsdppservice.entity.FeatureEngineering;
import com.synapxnet.mlopsdppservice.entity.FeaturePipelineParams;
import com.synapxnet.mlopsdppservice.entity.FeatureTaskInfo;
import com.synapxnet.mlopsdppservice.entity.JenkinsBuildStatus;
import com.synapxnet.mlopsdppservice.mapper.FeatureEngineeringMapper;
import com.synapxnet.mlopsdppservice.service.FeatureJenkinsService;
import com.synapxnet.mlopsdppservice.service.FeatureJenkinsScheduleService;
import com.synapxnet.mlopsdppservice.service.FeatureTaskInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dpp/feature-engineering")
public class FeatureEngineeringController {

    private static final Logger logger = LoggerFactory.getLogger(FeatureEngineeringController.class);

    private final FeatureEngineeringMapper featureEngineeringMapper;
    private final FeatureJenkinsService featureJenkinsService;
    private final FeatureJenkinsScheduleService featureJenkinsScheduleService;
    private final FeatureTaskInfoService featureTaskInfoService;

    @Autowired
    public FeatureEngineeringController(
            FeatureEngineeringMapper featureEngineeringMapper,
            FeatureJenkinsService featureJenkinsService,
            FeatureJenkinsScheduleService featureJenkinsScheduleService,
            FeatureTaskInfoService featureTaskInfoService) {
        this.featureEngineeringMapper = featureEngineeringMapper;
        this.featureJenkinsService = featureJenkinsService;
        this.featureJenkinsScheduleService = featureJenkinsScheduleService;
        this.featureTaskInfoService = featureTaskInfoService;
    }

    // 获取所有特征工程任务
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        try {
            List<FeatureEngineering> list = featureEngineeringMapper.findAll();
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", list,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取特征工程列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取特征工程列表失败",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 根据团队获取特征工程任务
    @GetMapping("/team/{teamUid}")
    public ResponseEntity<Map<String, Object>> listByTeam(@PathVariable("teamUid") String teamUid) {
        try {
            List<FeatureEngineering> list = featureEngineeringMapper.findByTeamUid(teamUid);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", list,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取团队特征工程列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取特征工程列表失败",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 根据ID获取特征工程
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable("id") Long id) {
        try {
            FeatureEngineering fe = featureEngineeringMapper.findById(id);
            if (fe != null) {
                return ResponseEntity.ok(Map.of(
                        "code", 0,
                        "message", "ok",
                        "data", fe,
                        "error", "null"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }
        } catch (Exception e) {
            logger.error("获取特征工程失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取特征工程失败",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 创建特征工程任务
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody FeatureEngineering featureEngineering,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            // 校验用户ID
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 401,
                        "message", "用户未登录或缺少用户信息",
                        "data", "null",
                        "error", "Missing X-User-Id header"
                ));
            }

            // 验证名称格式：必须以字母开头，只能包含字母、数字、下划线和中划线
            String name = featureEngineering.getName();
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "特征工程名称不能为空",
                        "data", "null",
                        "error", "Name is required"
                ));
            }
            if (!name.matches("^[a-zA-Z][a-zA-Z0-9_-]*$")) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "名称必须以字母开头，只能包含字母、数字、下划线(_)和中划线(-)",
                        "data", "null",
                        "error", "Invalid name format"
                ));
            }
            if (name.length() > 50) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "名称长度不能超过50个字符",
                        "data", "null",
                        "error", "Name too long"
                ));
            }

            // 检查名称是否重复
            if (featureEngineeringMapper.countByName(featureEngineering.getName()) > 0) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "特征工程名称已存在",
                        "data", "null",
                        "error", "Duplicate name"
                ));
            }

            // 生成UID
            featureEngineering.setUid("FE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            if (featureEngineering.getStatus() == null) {
                featureEngineering.setStatus("draft");
            }
            // 设置创建者
            featureEngineering.setCreatedBy(userId);

            featureEngineeringMapper.insert(featureEngineering);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "创建成功",
                    "data", featureEngineering,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("创建特征工程失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "创建特征工程失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 更新特征工程任务
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(@PathVariable("id") Long id, @RequestBody FeatureEngineering featureEngineering) {
        try {
            FeatureEngineering existing = featureEngineeringMapper.findById(id);
            if (existing == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            // 检查名称是否重复
            if (featureEngineeringMapper.countByNameExcludeId(featureEngineering.getName(), id) > 0) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "特征工程名称已存在",
                        "data", "null",
                        "error", "Duplicate name"
                ));
            }

            featureEngineering.setId(id);
            featureEngineeringMapper.update(featureEngineering);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", featureEngineering,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("更新特征工程失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "更新特征工程失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 删除特征工程任务
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Long id) {
        try {
            FeatureEngineering existing = featureEngineeringMapper.findById(id);
            if (existing == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            // 如果有 Jenkins Job，尝试删除
            if (existing.getJobUid() != null && !existing.getJobUid().isEmpty()) {
                try {
                    featureJenkinsService.deleteJob(existing.getJobUid());
                } catch (Exception e) {
                    logger.warn("删除 Jenkins 作业失败: {}", e.getMessage());
                }
            }

            // 删除相关的任务执行记录
            featureTaskInfoService.deleteByTaskUid(existing.getUid());

            featureEngineeringMapper.deleteById(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "删除成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("删除特征工程失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "删除特征工程失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 立即执行特征工程任务
    @PostMapping("/{id}/execute")
    @Transactional
    public ResponseEntity<Map<String, Object>> execute(@PathVariable("id") Long id) {
        try {
            FeatureEngineering fe = featureEngineeringMapper.findById(id);
            if (fe == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            // 构建 Pipeline 参数
            FeaturePipelineParams params = buildPipelineParams(fe);

            // 生成 Jenkins Job 路径
            String jobPath = "xnet-mlops-dpp/feature-engineering/" + fe.getName();

            // 检查 Job 是否存在，不存在则创建
            if (!featureJenkinsService.jobExists(jobPath)) {
                logger.info("创建 Jenkins Job: {}", jobPath);
                featureJenkinsService.createDynamicJob(jobPath, params);
            }

            // 触发构建
            String queueUrl = featureJenkinsService.triggerBuild(jobPath, fe.getUid());

            // 更新状态和 Job UID
            fe.setStatus("processing");
            fe.setJobUid(jobPath);
            fe.setLastBuildStatus("QUEUED");
            featureEngineeringMapper.update(fe);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "执行成功，任务已提交",
                    "data", Map.of(
                            "jobPath", jobPath,
                            "queueUrl", queueUrl,
                            "status", "QUEUED"
                    ),
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("执行特征工程失败", e);
            featureEngineeringMapper.updateStatus(id, "failed");
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "执行特征工程失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 创建调度任务
    @PostMapping("/{id}/schedule")
    @Transactional
    public ResponseEntity<Map<String, Object>> createSchedule(@PathVariable("id") Long id) {
        try {
            FeatureEngineering fe = featureEngineeringMapper.findById(id);
            if (fe == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            // 检查是否配置了调度
            if (fe.getScheduleConfig() == null || fe.getScheduleConfig().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "未配置调度信息",
                        "data", "null",
                        "error", "Schedule config is required"
                ));
            }

            // 构建 Pipeline 参数
            FeaturePipelineParams params = buildPipelineParams(fe);

            // 生成调度 Jenkins Job 路径
            String jobPath = "xnet-mlops-dpp/feature-engineering-schedule/" + fe.getName();

            // 检查 Job 是否存在，不存在则创建
            if (!featureJenkinsScheduleService.jobExists(jobPath)) {
                logger.info("创建调度 Jenkins Job: {}", jobPath);
                featureJenkinsScheduleService.createDynamicJob(jobPath, params);
            }

            // 更新调度状态
            fe.setScheduleActive(true);
            fe.setJobUid(jobPath);
            featureEngineeringMapper.update(fe);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "调度任务创建成功",
                    "data", Map.of(
                            "jobPath", jobPath,
                            "scheduleActive", true
                    ),
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("创建调度任务失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "创建调度任务失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 停止调度任务
    @PostMapping("/{id}/stop-schedule")
    @Transactional
    public ResponseEntity<Map<String, Object>> stopSchedule(@PathVariable("id") Long id) {
        try {
            FeatureEngineering fe = featureEngineeringMapper.findById(id);
            if (fe == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            // 停止调度（删除 Jenkins Job）
            if (fe.getJobUid() != null && !fe.getJobUid().isEmpty()) {
                try {
                    featureJenkinsScheduleService.deleteJob(fe.getJobUid());
                } catch (Exception e) {
                    logger.warn("删除调度 Jenkins Job 失败: {}", e.getMessage());
                }
            }

            // 更新调度状态
            fe.setScheduleActive(false);
            featureEngineeringMapper.update(fe);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "调度任务已停止",
                    "data", Map.of("scheduleActive", false),
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("停止调度任务失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "停止调度任务失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 停止当前构建
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopBuild(@PathVariable("id") Long id) {
        try {
            FeatureEngineering fe = featureEngineeringMapper.findById(id);
            if (fe == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            if (fe.getJobUid() == null || fe.getJobUid().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "没有运行中的任务",
                        "data", "null",
                        "error", "No running job"
                ));
            }

            boolean stopped = featureJenkinsService.stopBuild(fe.getJobUid());

            if (stopped) {
                fe.setStatus("stopped");
                fe.setLastBuildStatus("ABORTED");
                featureEngineeringMapper.update(fe);
            }

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", stopped ? "任务已停止" : "停止任务失败",
                    "data", Map.of("stopped", stopped),
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("停止任务失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "停止任务失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 获取构建状态
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getBuildStatus(@PathVariable("id") Long id) {
        try {
            FeatureEngineering fe = featureEngineeringMapper.findById(id);
            if (fe == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            if (fe.getJobUid() == null || fe.getJobUid().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 0,
                        "message", "ok",
                        "data", Map.of(
                                "status", fe.getStatus(),
                                "hasJob", false
                        ),
                        "error", "null"
                ));
            }

            // 先从 Redis 获取状态
            JenkinsBuildStatus status = featureJenkinsService.getBuildStatus(fe.getJobUid());

            if (status == null) {
                // 从 Jenkins 获取
                try {
                    status = featureJenkinsService.getBuildStatusFromJenkins(fe.getJobUid());
                } catch (Exception e) {
                    logger.warn("从 Jenkins 获取状态失败: {}", e.getMessage());
                }
            }

            if (status != null) {
                // 更新特征工程状态
                String overallStatus = status.getOverallStatus();
                if ("SUCCESS".equals(overallStatus)) {
                    fe.setStatus("completed");
                } else if ("FAILURE".equals(overallStatus)) {
                    fe.setStatus("failed");
                } else if ("IN_PROGRESS".equals(overallStatus) || "QUEUED".equals(overallStatus)) {
                    fe.setStatus("processing");
                }
                fe.setLastBuildStatus(overallStatus);
                featureEngineeringMapper.update(fe);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("status", fe.getStatus());
            data.put("lastBuildStatus", fe.getLastBuildStatus());
            data.put("hasJob", true);
            data.put("jobUid", fe.getJobUid());
            if (status != null) {
                data.put("buildStatus", status);
            }

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", data,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取构建状态失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取构建状态失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 获取任务执行历史
    @GetMapping("/{id}/history")
    public ResponseEntity<Map<String, Object>> getExecutionHistory(@PathVariable("id") Long id) {
        try {
            FeatureEngineering fe = featureEngineeringMapper.findById(id);
            if (fe == null) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "特征工程任务不存在",
                        "data", "null",
                        "error", "FeatureEngineering not found"
                ));
            }

            List<FeatureTaskInfo> history = featureTaskInfoService.findByTaskUid(fe.getUid());

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", history,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取执行历史失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取执行历史失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 构建 Pipeline 参数
    private FeaturePipelineParams buildPipelineParams(FeatureEngineering fe) {
        FeaturePipelineParams params = new FeaturePipelineParams();

        params.setDescription(fe.getDescription());
        params.setTaskName(fe.getName());
        params.setTaskUid(fe.getUid());

        // 数据源配置
        params.setDatasourceName(fe.getDatasourceName());
        params.setDatabase(fe.getDatabase());
        params.setTableName(fe.getTableName());
        params.setSelectedColumns(fe.getSelectedColumns());

        // 特征工程配置
        params.setOperatorCode(fe.getOperatorCode());
        params.setOperatorName(fe.getOperatorName());
        params.setOutputFormat(fe.getOutputFormat());
        params.setFeatureConfig(fe.getFeatureConfig());
        params.setOperatorParams(fe.getOperatorParams());

        // 输出配置
        params.setPushToDataset(fe.getPushToDataset());
        params.setTargetDatasetName(fe.getTargetDatasetName());
        params.setOutputPath(fe.getOutputPath());
        params.setEncryption(fe.getEncryption());

        // 存储配置
        params.setZone(fe.getZone());
        params.setBucketUid(fe.getBucketUid());
        params.setBucketName(fe.getBucketName());

        // Docker镜像配置
        params.setDockerImageName(fe.getImageName());
        params.setDockerImageTag(fe.getImageTag());
        params.setHarborUrl(fe.getHarborUrl());
        params.setHarborCredentialsId(fe.getHarborCredentialsId());

        // 调度配置
        params.setScheduleConfig(fe.getScheduleConfig());

        return params;
    }
}
