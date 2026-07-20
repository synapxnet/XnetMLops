package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.config.PipelineParamEnricher;
import com.synapxnet.mlopsmtpservice.entity.JenkinsBuildStatus;
import com.synapxnet.mlopsmtpservice.entity.PipelineConfigParams;
import com.synapxnet.mlopsmtpservice.entity.TaskInfo;
import com.synapxnet.mlopsmtpservice.entity.TrainTask;
import com.synapxnet.mlopsmtpservice.service.*;
import com.synapxnet.mlopsmtpservice.service.JenkinsService;
import com.synapxnet.mlopsmtpservice.service.JenkinsScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.CronExpression;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/mtp")
public class JobController {

    private final JenkinsService jenkinsService;
    private final JenkinsScheduleService jenkinsScheduleService;
    private final TrainTaskService trainTaskService;
    private final PipelineParamEnricher paramEnricher;
    private final TaskInfoService taskInfoService;
    private final RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskScheduleInfoService taskScheduleInfoService;


    @Autowired
    public JobController(JenkinsService jenkinsService,
                         JenkinsScheduleService jenkinsScheduleService,
                         TrainTaskService trainTaskService,
                         PipelineParamEnricher paramEnricher,
                         TaskInfoService taskInfoService,
                         RedisTemplate<String, String> redisTemplate
    )
    {
        this.jenkinsService = jenkinsService;
        this.jenkinsScheduleService = jenkinsScheduleService;
        this.trainTaskService = trainTaskService;
        this.paramEnricher = paramEnricher;
        this.taskInfoService = taskInfoService;
        this.redisTemplate = redisTemplate;
    }

    // 响应工具类 (与TrainTaskController一致)
    private static class ResponseUtils {
        static ResponseEntity<Map<String, Object>> success(Object data) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "success",
                    "data", data,
                    "error", "null"
            ));
        }

        static ResponseEntity<Map<String, Object>> success(String message, Object data) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", message,
                    "data", data,
                    "error", "null"
            ));
        }

        static ResponseEntity<Map<String, Object>> error(int code, String message) {
            return ResponseEntity.ok(Map.of(
                    "code", code,
                    "message", message,
                    "data", "null",
                    "error", message
            ));
        }

        static ResponseEntity<Map<String, Object>> error(int code, String message, String error) {
            return ResponseEntity.ok(Map.of(
                    "code", code,
                    "message", message,
                    "data", "null",
                    "error", error
            ));
        }
    }

    @PostMapping("/pipeline")
    public ResponseEntity<Map<String, Object>> createPipeline(
            @RequestParam String taskUID,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestBody PipelineConfigParams params) {
        try {
            String jobUID = UUID.randomUUID().toString();
            TrainTask task = trainTaskService.findByUidAndTenantUid(taskUID, tenantUid)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskUID));
            paramEnricher.enrichParamsWithTaskInfo(params, task, jobUID);

            // 创建并触发构建 - 修改后调用带两个参数的triggerBuild
            String queueUrl = jenkinsService.createDynamicJob(jobUID, params);
            jenkinsService.triggerBuild(jobUID, taskUID); // 传入jobUID和taskUID

            return ResponseUtils.success("流水线创建成功", Map.of(
                    "jobName", jobUID,
                    "queueUrl", queueUrl,
                    "monitorUrl", URLEncoder.encode(taskUID, StandardCharsets.UTF_8)
            ));
        } catch (Exception e) {
            return ResponseUtils.error(500, "创建流水线失败", e.getMessage());
        }
    }

    // 新增构建状态查询端点
    @GetMapping("/build/status/{jobName}")
    public ResponseEntity<Map<String, Object>> getBuildStatus(
            @PathVariable String jobName,
            @RequestParam(required = false, defaultValue = "false") boolean includeConsole) {

        try {
            // 从Redis获取构建状态
            JenkinsBuildStatus status = jenkinsService.getBuildStatusFromJenkins(jobName);

            if (status == null) {
                return ResponseUtils.error(404, "构建信息不存在");
            }

            // 构建响应数据
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jobName", status.getJobName());
            response.put("buildUrl", status.getBuildUrl());
            response.put("queueUrl", status.getQueueUrl());
            response.put("overallStatus", status.getOverallStatus());

            // 格式化阶段信息
            List<Map<String, Object>> stages = new ArrayList<>();
            for (JenkinsBuildStatus.StageInfo stage : status.getStages()) {
                Map<String, Object> stageInfo = new LinkedHashMap<>();
                stageInfo.put("stageName", stage.getStageName());
                stageInfo.put("status", stage.getStatus());
                stageInfo.put("durationMillis", stage.getDurationMillis());
                stageInfo.put("startTime", stage.getStartTime());

                // 格式化持续时间 (HH:mm:ss)
                stageInfo.put("durationFormatted", formatDuration(stage.getDurationMillis()));

                stages.add(stageInfo);
            }
            response.put("stages", stages);

            if (includeConsole) {
                response.put("consoleOutput", status.getConsoleOutput());
            }

            return ResponseUtils.success(response);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取构建状态失败", e.getMessage());
        }
    }

    // 格式化持续时间
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        return String.format("%02d:%02d:%02d",
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60);
    }

    // 新增接口：根据task_uid获取所有作业
    @GetMapping("/task/{taskUid}/jobs")
    public ResponseEntity<Map<String, Object>> getJobsByTaskUid(
            @PathVariable String taskUid) {
        try {
            List<TaskInfo> jobs = taskInfoService.getJobsByTaskUid(taskUid);

            // 构建响应数据
            List<Map<String, Object>> responseList = new ArrayList<>();
            for (TaskInfo job : jobs) {
                Map<String, Object> jobInfo = new LinkedHashMap<>();
                jobInfo.put("jobUid", job.getJob_uid());
                jobInfo.put("jobStatus", job.getJob_status());
                jobInfo.put("startAt", job.getStart_at());
                jobInfo.put("endAt", job.getEnd_at());
                jobInfo.put("scheduleActive", job.getSchedule_active());
                responseList.add(jobInfo);
            }

            return ResponseUtils.success(responseList);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取任务作业列表失败", e.getMessage());
        }
    }

    // 新增接口：根据job_uid获取作业详情
    @GetMapping("/job/{jobUid}")
    public ResponseEntity<Map<String, Object>> getJobByJobUid(
            @PathVariable String jobUid) {
        try {
            TaskInfo job = taskInfoService.getJobByJobUid(jobUid);

            if (job == null) {
                return ResponseUtils.error(404, "作业不存在");
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jobUid", job.getJob_uid());
            response.put("jobStatus", job.getJob_status());
            response.put("jobContent", job.getJob_content());
            response.put("startAt", job.getStart_at());
            response.put("endAt", job.getEnd_at());
            response.put("scheduleActive", job.getSchedule_active());
            //System.out.println("jobContent " +  response);

            return ResponseUtils.success(response);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取作业详情失败", e.getMessage());
        }
    }

    @DeleteMapping("/job/{jobUid}")
    public ResponseEntity<Map<String, Object>> deleteJob(
            @PathVariable String jobUid,
            @RequestParam(required = false, defaultValue = "false") boolean forceStop) {

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("jobUid", jobUid);

            // 停止构建
            if (forceStop) {
                boolean stopped = jenkinsService.stopBuild(jobUid);
                result.put("forceStop", stopped);
                if (stopped) {
                    Thread.sleep(2000);
                }
            }

            // 删除 Jenkins 任务
            boolean jenkinsDeleted = jenkinsService.deleteJob(jobUid);
            result.put("jenkinsDeleted", jenkinsDeleted);

            // 删除数据库记录
            boolean dbDeleted = taskInfoService.deleteByJobUid(jobUid);
            result.put("dbDeleted", dbDeleted);

            // 清理 Redis 缓存
            redisTemplate.delete(jobUid);

            return ResponseUtils.success("删除成功", result);

        } catch (Exception e) {
            return ResponseUtils.error(500, "删除失败", e.getMessage());
        }
    }

    // 可选：批量删除接口
    @DeleteMapping("/task/{taskUid}/jobs")
    public ResponseEntity<Map<String, Object>> deleteJobsByTaskUid(
            @PathVariable String taskUid) {

        try {
            List<TaskInfo> jobs = taskInfoService.getJobsByTaskUid(taskUid);

            if (jobs.isEmpty()) {
                return ResponseUtils.success("没有找到相关作业", Map.of("taskUid", taskUid));
            }

            List<String> results = new ArrayList<>();
            for (TaskInfo job : jobs) {
                String jobUid = job.getJob_uid();
                try {
                    jenkinsService.deleteJob(jobUid);
                    taskInfoService.deleteByJobUid(jobUid);
                    redisTemplate.delete(jobUid);
                    results.add(jobUid + ": 删除成功");
                } catch (Exception e) {
                    results.add(jobUid + ": 删除失败 - " + e.getMessage());
                }
            }

            return ResponseUtils.success("批量删除完成", Map.of(
                    "taskUid", taskUid,
                    "totalJobs", jobs.size(),
                    "results", results
            ));

        } catch (Exception e) {
            return ResponseUtils.error(500, "批量删除失败", e.getMessage());
        }
    }

    /**
     * 创建调度流水线
     */
    @PostMapping("/schedule/pipeline")
    public ResponseEntity<Map<String, Object>> startSchedule(
            @RequestParam String taskUID,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestBody Map<String, Object> requestBody) {

        try {
            // 从请求体中获取调度配置
            Object scheduleConfigObj = requestBody.get("scheduleConfig");
            if (scheduleConfigObj == null) {
                return ResponseUtils.error(400, "调度配置不能为空");
            }

            String scheduleConfigJson;
            if (scheduleConfigObj instanceof String) {
                scheduleConfigJson = (String) scheduleConfigObj;
            } else {
                scheduleConfigJson = objectMapper.writeValueAsString(scheduleConfigObj);
            }

            // 检查调度是否激活
            boolean scheduleActive = scheduleService.isScheduleActive(scheduleConfigJson);

            TrainTask task = trainTaskService.findByUidAndTenantUid(taskUID, tenantUid)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskUID ));

            // 创建PipelineConfigParams
            PipelineConfigParams params = new PipelineConfigParams();

            // 设置调度配置
            params.setScheduleConfig(scheduleConfigJson);

            String jobUID = UUID.randomUUID().toString();
            paramEnricher.enrichParamsWithTaskInfo(params, task, jobUID);

            // 生成调度版的流水线
            String scheduleJobName = jobUID;
            String queueUrl = jenkinsScheduleService.createDynamicJob(scheduleJobName, params);
            jenkinsScheduleService.triggerBuild(jobUID, taskUID);

            return ResponseUtils.success("调度任务创建成功", Map.of(
                    "jobName", scheduleJobName,
                    "scheduleActive", scheduleActive,
                    "cronExpression", scheduleService.convertToCronExpression(scheduleConfigJson),
                    "queueUrl", queueUrl
            ));

        } catch (Exception e) {
            return ResponseUtils.error(500, "创建调度任务失败", e.getMessage());
        }
    }

    /**
     * 结束调度任务
     */
    @DeleteMapping("/schedule/stop/{jobName}")
    public ResponseEntity<Map<String, Object>> stopSchedule(
            @PathVariable String jobName,
            @RequestParam(required = false, defaultValue = "false") boolean deleteJob) {

        try {
            Map<String, Object> result = new HashMap<>();

            // 1. 停止所有相关的构建
            boolean allStopped = stopAllBuilds(jobName);
            result.put("buildsStopped", allStopped);

            // 2. 删除调度配置
            boolean scheduleConfigDeleted = deleteScheduleConfig(jobName);
            result.put("scheduleConfigDeleted", scheduleConfigDeleted);

            // 3. 可选：删除Jenkins任务
            if (deleteJob) {
                boolean jobDeleted = jenkinsScheduleService.deleteJob(jobName);
                result.put("jobDeleted", jobDeleted);

                // 清理相关记录
                taskScheduleInfoService.deleteByJobUid(jobName);
                redisTemplate.delete(jobName);
            }

            return ResponseUtils.success("调度任务已停止", result);

        } catch (Exception e) {
            return ResponseUtils.error(500, "停止调度任务失败", e.getMessage());
        }
    }

    /**
     * 获取调度任务状态
     */
    @GetMapping("/schedule/status/{jobName}")
    public ResponseEntity<Map<String, Object>> getScheduleStatus(@PathVariable String jobName) {
        try {
            // 获取调度配置
            Map<String, Object> scheduleConfig = getScheduleConfigFromDb(jobName);
//            if (scheduleConfig == null) {
//                return ResponseUtils.error(404, "调度配置不存在");
//            }
            // 获取构建状态
            JenkinsBuildStatus buildStatus = jenkinsScheduleService.getBuildStatusFromJenkins(jobName);
            Map<String, Object> buildStatusList =  jenkinsScheduleService.getAllBuildStatusFromJenkins(jobName);
            System.out.println(buildStatus);
            // 构建响应
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jobName", jobName);
            response.put("scheduleConfig", scheduleConfig);

            if (buildStatus != null) {
                response.put("nextBuildNumber", buildStatusList.get("nextBuildNumber"));
                response.put("jobHistoryBuild", buildStatusList.get("jobHistoryBuild"));
            }

            // 检查下一次执行时间
//            String cronExpression = (String) scheduleConfig.get("cronExpression");
//            if (cronExpression != null && !cronExpression.isEmpty()) {
//                response.put("nextScheduleTime", calculateNextExecution(cronExpression));
//            }

            return ResponseUtils.success(response);

        } catch (Exception e) {
            return ResponseUtils.error(500, "获取调度状态失败", e.getMessage());
        }
    }

    /**
     * 更新调度配置
     */
    @PutMapping("/schedule/update/{jobName}")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable String jobName,
            @RequestBody Map<String, Object> newScheduleConfig) {

        try {
            // 验证调度配置
            if (!newScheduleConfig.containsKey("isActive")) {
                return ResponseUtils.error(400, "调度配置必须包含isActive字段");
            }

            // 停止当前调度
            stopSchedule(jobName, false);

            // 更新调度配置
            boolean updated = updateScheduleConfig(jobName, newScheduleConfig);

            if (!updated) {
                return ResponseUtils.error(500, "更新调度配置失败");
            }

            // 重新启动调度
            boolean restarted = restartScheduleWithNewConfig(jobName, newScheduleConfig);

            return ResponseUtils.success("调度配置更新成功", Map.of(
                    "jobName", jobName,
                    "updated", updated,
                    "restarted", restarted
            ));

        } catch (Exception e) {
            return ResponseUtils.error(500, "更新调度配置失败", e.getMessage());
        }
    }


    @GetMapping("/schedule/{taskUid}/jobs")
    public ResponseEntity<Map<String, Object>> getJobsScheduleByTaskUid(
            @PathVariable String taskUid) {
        try {
            List<TaskInfo> jobs = taskScheduleInfoService.getJobsByTaskUid(taskUid);

            // 构建响应数据
            List<Map<String, Object>> responseList = new ArrayList<>();
            for (TaskInfo job : jobs) {
                Map<String, Object> jobInfo = new LinkedHashMap<>();
                jobInfo.put("jobUid", job.getJob_uid());
                jobInfo.put("jobStatus", job.getJob_status());
                jobInfo.put("startAt", job.getStart_at());
                jobInfo.put("endAt", job.getEnd_at());
                jobInfo.put("scheduleActive", job.getSchedule_active());
                responseList.add(jobInfo);
            }

            return ResponseUtils.success(responseList);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取任务作业列表失败", e.getMessage());
        }
    }

    @GetMapping("/schedule/{jobUid}")
    public ResponseEntity<Map<String, Object>> getJobScheduleByJobUid(
            @PathVariable String jobUid) {
        try {
            TaskInfo job = taskScheduleInfoService.getJobByJobUid(jobUid);

            if (job == null) {
                return ResponseUtils.error(404, "作业不存在");
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jobUid", job.getJob_uid());
            response.put("jobStatus", job.getJob_status());
            response.put("jobContent", job.getJob_content());
            response.put("startAt", job.getStart_at());
            response.put("endAt", job.getEnd_at());
            response.put("scheduleActive", job.getSchedule_active());

            //System.out.println("jobContent " +  response);

            return ResponseUtils.success(response);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取作业详情失败", e.getMessage());
        }
    }

    private boolean stopAllBuilds(String jobName) {
        try {
            // 获取作业的所有构建并停止
            return jenkinsScheduleService.stopBuild(jobName);
        } catch (Exception e) {
            System.err.println("停止构建失败: {}" + e.getMessage());
            return false;
        }
    }

    private boolean deleteScheduleConfig(String jobName) {
        try {
            // 从Redis删除调度配置
            redisTemplate.delete("schedule:" + jobName);

            // 从数据库删除调度配置
            // 这里需要根据实际的数据存储方式实现
            return true;
        } catch (Exception e) {
            System.err.println("删除调度配置失败: {}" + e.getMessage());
            return false;
        }
    }

    private Map<String, Object> getScheduleConfigFromDb(String jobName) {
        try {
            String configJson = redisTemplate.opsForValue().get("schedule:" + jobName);
            if (configJson != null) {
                return objectMapper.readValue(configJson, Map.class);
            }
            return null;
        } catch (Exception e) {
            System.err.println("获取调度配置失败: {}" + e.getMessage());
            return null;
        }
    }

    private boolean updateScheduleConfig(String jobName, Map<String, Object> config) {
        try {
            String configJson = objectMapper.writeValueAsString(config);
            redisTemplate.opsForValue().set(
                    "schedule:" + jobName,
                    configJson,
                    30, TimeUnit.DAYS
            );
            return true;
        } catch (Exception e) {
            System.err.println("更新调度配置失败: {}" + e.getMessage());
            return false;
        }
    }

    private boolean restartScheduleWithNewConfig(String jobName, Map<String, Object> config) {
        try {
            // 根据新的配置重新配置Jenkins任务
            // 这里需要实现具体的重新配置逻辑
            return true;
        } catch (Exception e) {
            System.err.println("重启调度失败: {}" + e.getMessage());
            return false;
        }
    }

    private String calculateNextExecution(String cronExpression) {
        try {
            if (cronExpression == null || cronExpression.trim().isEmpty()) {
                return "无计划任务";
            }

            // 创建 CronExpression 对象
            CronExpression cron = new CronExpression(cronExpression);

            // 设置时区（默认为系统时区，可根据需要调整）
            cron.setTimeZone(TimeZone.getDefault());

            // 获取当前时间
            Date currentTime = new Date();

            // 计算下一次执行时间
            Date nextExecution = cron.getNextValidTimeAfter(currentTime);

            if (nextExecution == null) {
                return "无下一次执行时间";
            }

            // 格式化输出
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(nextExecution);

        } catch (ParseException e) {
            // Cron 表达式格式错误
            return "Cron表达式格式错误: " + e.getMessage();
        } catch (Exception e) {
            return "无法计算: " + e.getMessage();
        }
    }

}