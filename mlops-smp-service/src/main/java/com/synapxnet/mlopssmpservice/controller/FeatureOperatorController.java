package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import com.synapxnet.mlopssmpservice.service.FeatureOperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smp/feature-operators")
public class FeatureOperatorController {

    private final FeatureOperatorService featureOperatorService;

    @Autowired
    public FeatureOperatorController(FeatureOperatorService featureOperatorService) {
        this.featureOperatorService = featureOperatorService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOperator(@RequestBody FeatureOperator featureOperator) {
        try {
            FeatureOperator created = featureOperatorService.createOperator(featureOperator);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "创建成功",
                    "data", created,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "创建失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOperator(
            @PathVariable Long id,
            @RequestBody FeatureOperator featureOperator) {
        try {
            FeatureOperator updated = featureOperatorService.updateOperator(id, featureOperator);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", updated,
                    "error", "null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "更新失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteOperator(@PathVariable Long id) {
        try {
            featureOperatorService.deleteOperator(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "删除成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "删除失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOperatorById(@PathVariable Long id) {
        try {
            FeatureOperator featureOperator = featureOperatorService.getOperatorById(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", featureOperator,
                    "error", "null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/uid/{uid}")
    public ResponseEntity<Map<String, Object>> getOperatorByUid(@PathVariable String uid) {
        try {
            FeatureOperator featureOperator = featureOperatorService.getOperatorByUid(uid);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", featureOperator,
                    "error", "null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOperators(
            @RequestParam(required = false) String tenantUid,
            @RequestParam(required = false) String deptUid,
            @RequestParam(required = false) String operatorName) {
        try {
            List<FeatureOperator> operators = featureOperatorService.searchOperators(tenantUid, deptUid, operatorName);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", operators,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "查询失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledOperators() {
        try {
            List<FeatureOperator> operators = featureOperatorService.getAllOperators();
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", operators,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "查询失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchOperators(
            @RequestParam(required = false) String tenantUid,
            @RequestParam(required = false) String deptUid,
            @RequestParam(required = false) String teamUid,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String operatorCode,
            @RequestParam(required = false) String version) {
        try {
            List<FeatureOperator> operators = featureOperatorService.searchOperators(
                    tenantUid, deptUid, teamUid, operatorName, operatorCode, version
            );
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", operators,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "搜索失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/code/{operatorCode}")
    public ResponseEntity<Map<String, Object>> getOperatorsByCode(@PathVariable String operatorCode) {
        try {
            List<FeatureOperator> operators = featureOperatorService.getOperatorsByCode(operatorCode);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", operators,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "查询失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }
}
