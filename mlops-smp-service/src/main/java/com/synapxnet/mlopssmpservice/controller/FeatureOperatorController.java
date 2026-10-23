package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import com.synapxnet.mlopssmpservice.service.FeatureOperatorService;
import com.synapxnet.mlopssmpservice.service.SmpResourceAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smp/feature-operators")
public class FeatureOperatorController {

    private final FeatureOperatorService featureOperatorService;
    private final SmpResourceAccessService resourceAccess;

    @Autowired
    public FeatureOperatorController(FeatureOperatorService featureOperatorService, SmpResourceAccessService resourceAccess) {
        this.featureOperatorService = featureOperatorService;
        this.resourceAccess = resourceAccess;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOperator(@RequestBody FeatureOperator featureOperator, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        scope.requireMatching(featureOperator.getTenant_uid(), featureOperator.getDept_uid(), featureOperator.getTeam_uid());
        featureOperator.setTenant_uid(scope.tenantUid());
        featureOperator.setDept_uid(scope.deptUid());
        featureOperator.setTeam_uid(scope.teamUid());
        featureOperator.setCreated_by(scope.userId());
        featureOperator.setUpdated_by(scope.userId());
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
            @RequestBody FeatureOperator featureOperator, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        scope.requireMatching(featureOperator.getTenant_uid(), featureOperator.getDept_uid(), featureOperator.getTeam_uid());
        featureOperator.setUpdated_by(scope.userId());
        try {
            scope.requireResource(featureOperatorService.getOperatorById(id));
            FeatureOperator updated = featureOperatorService.updateOperator(id, featureOperator);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", updated,
                    "error", "null"
            ));
        } catch (ResponseStatusException denied) {
            throw denied;
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
    public ResponseEntity<Map<String, Object>> deleteOperator(@PathVariable Long id, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        try {
            scope.requireResource(featureOperatorService.getOperatorById(id));
            featureOperatorService.deleteOperator(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "删除成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (ResponseStatusException denied) {
            throw denied;
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
    public ResponseEntity<Map<String, Object>> getOperatorById(@PathVariable Long id, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        try {
            FeatureOperator featureOperator = featureOperatorService.getOperatorById(id);
            scope.requireResource(featureOperator);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", featureOperator,
                    "error", "null"
            ));
        } catch (ResponseStatusException denied) {
            throw denied;
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
    public ResponseEntity<Map<String, Object>> getOperatorByUid(@PathVariable String uid, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        try {
            FeatureOperator featureOperator = featureOperatorService.getOperatorByUid(uid);
            scope.requireResource(featureOperator);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", featureOperator,
                    "error", "null"
            ));
        } catch (ResponseStatusException denied) {
            throw denied;
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
            @RequestParam(required = false) String teamUid,
            @RequestParam(required = false) String operatorName, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        scope.requireMatching(tenantUid, deptUid, teamUid);
        try {
            List<FeatureOperator> operators = featureOperatorService.searchOperators(scope.tenantUid(), scope.deptUid(), scope.teamUid(), operatorName, null, null);
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
    public ResponseEntity<Map<String, Object>> getEnabledOperators(HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        try {
            List<FeatureOperator> operators = featureOperatorService.searchOperators(scope.tenantUid(), scope.deptUid(), scope.teamUid(), null, null, null);
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
            @RequestParam(required = false) String version, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        scope.requireMatching(tenantUid, deptUid, teamUid);
        try {
            List<FeatureOperator> operators = featureOperatorService.searchOperators(
                    scope.tenantUid(), scope.deptUid(), scope.teamUid(), operatorName, operatorCode, version
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
    public ResponseEntity<Map<String, Object>> getOperatorsByCode(@PathVariable String operatorCode, HttpServletRequest request) {
        var scope = resourceAccess.requireScope(request);
        try {
            List<FeatureOperator> operators = featureOperatorService.getOperatorsByCode(operatorCode).stream().filter(scope::contains).toList();
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
