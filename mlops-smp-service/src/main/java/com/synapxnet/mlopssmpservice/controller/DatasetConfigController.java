package com.synapxnet.mlopssmpservice.controller;


import com.synapxnet.mlopssmpservice.entity.ConfigItem;
import com.synapxnet.mlopssmpservice.entity.DatasetConfig;
import com.synapxnet.mlopssmpservice.mapper.DatasetConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/smp")
public class DatasetConfigController {
    @GetMapping("/test")
    public String getUsers() {
        return "user";
    }

    @Resource
    private DatasetConfigMapper datasetConfigMapper;

    //获取数据集配置
    @GetMapping("/dataset-config")
    public ResponseEntity<Map<String, Object>> getDatasetConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("datasetTypes", datasetConfigMapper.getDatasetTypes());
        data.put("datasetZones", datasetConfigMapper.getDatasetZones());

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", data,
                "error", "null"
        ));
    }
    //新增数据集配置
    @PostMapping("/dataset-config/{type}")
    public ResponseEntity<Map<String, Object>> addDatasetConfig(
            @PathVariable("type") String type,
            @RequestBody ConfigItem item) {

        // 验证type是否有效
        if (!"datasetTypes".equals(type) && !"datasetZones".equals(type)) {
            return ResponseEntity.ok(Map.of(
                    "code", 400,
                    "message", "Invalid type parameter. Must be 'datasetTypes' or 'datasetZones'",
                    "data", "null",
                    "error", "InvalidType"
            ));
        }

        // 转换type为数据库中的配置类型
        String configType = "datasetTypes".equals(type) ? "DATASET_TYPE" : "DATASET_ZONE";

        try {
            int result = datasetConfigMapper.insertDatasetConfig(
                    item.getLabel(),
                    item.getValue(),
                    configType
            );

            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                        "code", 0,
                        "message", "ok",
                        "data", "null",
                        "error", "null"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "code", -1,
                        "message", "Failed to add data",
                        "data", "null",
                        "error", "InsertFailed"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -2,
                    "message", "Server error",
                    "data", "null",
                    "error", e.getClass().getSimpleName()
            ));
        }
    }

    @DeleteMapping("/dataset-config/{type}/{value}")
    public ResponseEntity<Map<String, Object>> deleteDatasetConfig(
            @PathVariable("type") String type,
            @PathVariable("value") String value) {

        // 验证type是否有效
        if (!"datasetTypes".equals(type) && !"datasetZones".equals(type)) {
            return ResponseEntity.ok(Map.of(
                    "code", 400,
                    "message", "无效的类型参数",
                    "data", "null",  // 使用字符串 "null" 代替 null
                    "error", "InvalidType"
            ));
        }

        // 转换type为数据库中的配置类型
        String configType = "datasetTypes".equals(type) ? "DATASET_TYPE" : "DATASET_ZONE";

        try {
            // 执行删除操作
            int result = datasetConfigMapper.deleteDatasetConfig(configType, value);

            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                        "code", 0,
                        "message", "删除成功",
                        "data", "null",
                        "error", "null"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "code", -4,
                        "message", "未找到对应配置项",
                        "data", "null",
                        "error", "NotFound"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -2,
                    "message", "服务器错误: " + e.getMessage(),
                    "data", "null",
                    "error", e.getClass().getSimpleName()
            ));
        }
    }





}
