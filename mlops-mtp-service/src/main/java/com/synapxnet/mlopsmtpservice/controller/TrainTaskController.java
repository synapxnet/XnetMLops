package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.entity.TrainTask;
import com.synapxnet.mlopsmtpservice.service.ScheduleService;
import com.synapxnet.mlopsmtpservice.service.TrainTaskService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mtp")
public class TrainTaskController {
    private final TrainTaskService trainTaskService;
    @Autowired
    private ScheduleService scheduleService;


    private TrainTaskController(TrainTaskService trainTaskService) {
        this.trainTaskService = trainTaskService;

    }

    public static TrainTaskController createTrainTaskController(TrainTaskService trainTaskService) {
        return new TrainTaskController(trainTaskService);
    }

    // 响应工具类
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

    @PostMapping("/tasks-creat")
    public ResponseEntity<Map<String, Object>> createTask(
            @RequestBody TrainTask task,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestHeader("X-User-Id") String userId) {
        try {
            // 创建原始任务
            TrainTask createdTask = trainTaskService.createOrUpdateTask(task, userId, tenantUid);
            return ResponseUtils.success("训练任务创建成功", createdTask);
        } catch (Exception e) {
            return ResponseUtils.error(500, "创建训练任务失败", e.getMessage());
        }
    }



    @PutMapping("/tasks/{uid}")
    public ResponseEntity<Map<String, Object>> updateTask(
            @PathVariable String uid,
            @RequestBody TrainTask task,
            @RequestHeader("X-Tenant-Uid") String tenantUid,
            @RequestHeader("X-User-Id") String userId) {
        try {
            task.setUid(uid);
            System.out.println("Datasets " + task);
            TrainTask updatedTask = trainTaskService.createOrUpdateTask(task, userId, tenantUid);
            return ResponseUtils.success("训练任务更新成功", updatedTask);
        } catch (Exception e) {
            return ResponseUtils.error(500, "更新训练任务失败", e.getMessage());
        }
    }


    @GetMapping("/tasks/{uid}")
    public ResponseEntity<Map<String, Object>> getTaskByUid(
            @PathVariable String uid,
            @RequestHeader("X-Tenant-Uid") String tenantUid) {
        try {
            return trainTaskService.findByUidAndTenantUid(uid, tenantUid)
                    .map(ResponseUtils::success)
                    .orElseGet(() -> ResponseUtils.error(404, "训练任务不存在"));
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取训练任务失败", e.getMessage());
        }
    }
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getAllTasks(
            @RequestHeader("X-Tenant-Uid") String tenantUid) {
        try {
            List<TrainTask> tasks = trainTaskService.findAllByTenantUid(tenantUid);
            return ResponseUtils.success(tasks);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取训练任务列表失败", e.getMessage());
        }
    }

    @DeleteMapping("/tasks/{uid}")
    public ResponseEntity<Map<String, Object>> deleteTask(
            @PathVariable String uid,
            @RequestHeader("X-Tenant-Uid") String tenantUid) {
        try {
            trainTaskService.deleteTask(uid, tenantUid);
            return ResponseUtils.success("训练任务删除成功", "null");
        } catch (Exception e) {
            return ResponseUtils.error(500, "删除训练任务失败", e.getMessage());
        }
    }
}