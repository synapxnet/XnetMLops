package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.AlgorithmRepository;
import com.synapxnet.mlopssmpservice.service.AlgorithmRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smp/algorithms")
public class AlgorithmRepositoryController {

    private final AlgorithmRepositoryService algorithmService;

    @Autowired
    public AlgorithmRepositoryController(AlgorithmRepositoryService algorithmRepositoryService) {
        this.algorithmService = algorithmRepositoryService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAlgorithm(@RequestBody AlgorithmRepository algorithmRepository) {
        try {
            AlgorithmRepository created = algorithmService.createAlgorithm(algorithmRepository);
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
    public ResponseEntity<Map<String, Object>> updateAlgorithm(
            @PathVariable Long id,
            @RequestBody AlgorithmRepository algorithmRepository) {
        try {
            AlgorithmRepository updated = algorithmService.updateAlgorithm(id, algorithmRepository);
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
    public ResponseEntity<Map<String, Object>> deleteAlgorithm(@PathVariable Long id) {
        try {
            algorithmService.deleteAlgorithm(id);
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
    public ResponseEntity<Map<String, Object>> getAlgorithmById(@PathVariable Long id) {
        try {
            AlgorithmRepository algorithmRepository = algorithmService.getAlgorithmById(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", algorithmRepository,
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
    public ResponseEntity<Map<String, Object>> getAlgorithmByUid(@PathVariable String uid) {
        try {
            AlgorithmRepository algorithmRepository = algorithmService.getAlgorithmByUid(uid);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", algorithmRepository,
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
    public ResponseEntity<Map<String, Object>> getAllAlgorithms(
            @RequestParam(required = false) String tenantUid,
            @RequestParam(required = false) String deptUid,
            @RequestParam(required = false) String algorithmName) {
        try {
            List<AlgorithmRepository> algorithmRepositories = algorithmService.searchAlgorithms(tenantUid, deptUid, algorithmName);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", algorithmRepositories,
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
    public ResponseEntity<Map<String, Object>> searchAlgorithms(
            @RequestParam(required = false) String tenantUid,
            @RequestParam(required = false) String deptUid,
            @RequestParam(required = false) String teamUid,
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) String version) {
        try {
            List<AlgorithmRepository> algorithmRepositories = algorithmService.searchAlgorithms(tenantUid, deptUid, teamUid, algorithm, version);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", algorithmRepositories,
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
}