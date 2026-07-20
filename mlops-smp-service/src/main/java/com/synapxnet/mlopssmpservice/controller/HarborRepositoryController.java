package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.HarborRepository;
import com.synapxnet.mlopssmpservice.service.HarborRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/smp/harbor")
public class HarborRepositoryController {

    private final HarborRepositoryService repositoryService;

    @Autowired
    public HarborRepositoryController(HarborRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRepository(@RequestBody HarborRepository repository) {
        try {
            HarborRepository created = repositoryService.createRepository(repository);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "Harbor仓库创建成功",
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRepositories() {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", repositoryService.getAllRepositories(),
                "error", "null"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRepositoryById(@PathVariable Integer id) {
        Optional<HarborRepository> repository = repositoryService.getRepositoryById(id);
        if (repository.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", repository.get(),
                    "error", "null"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "仓库未找到",
                "data", "null",
                "error", "Repository not found"
        ));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateRepository(
            @PathVariable Integer id,
            @RequestBody HarborRepository repository) {
        try {
            HarborRepository updated = repositoryService.updateRepository(id, repository);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", updated,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -1,
                    "message", "更新失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteRepository(@PathVariable Integer id) {
        try {
            repositoryService.deleteRepository(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "删除成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", -1,
                    "message", "删除失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }
}
