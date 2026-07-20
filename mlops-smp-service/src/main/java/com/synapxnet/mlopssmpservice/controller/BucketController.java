package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.Bucket;
import com.synapxnet.mlopssmpservice.service.BucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/smp")
public class BucketController {

    private final BucketService bucketService;

    @Autowired
    public BucketController(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @GetMapping("/bucket")
    public ResponseEntity<Map<String, Object>> listAllBuckets() {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", bucketService.getAllBuckets(),
                "error", "null"
        ));
    }

    @PostMapping("/bucket")
    @Transactional
    public ResponseEntity<Map<String, Object>> saveBucket(@RequestBody Bucket bucket) {
        try {
            Bucket savedBucket = bucketService.saveBucket(bucket);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "保存成功",
                    "data", savedBucket,
                    "error", "null"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "保存失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/bucket/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteBucket(@PathVariable Long id) {
        try {
            if (bucketService.findBucketById(id).isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "存储桶不存在",
                        "data", "null",
                        "error", "Bucket not found"
                ));
            }

            boolean success = bucketService.deleteBucket(id);
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "code", 0,
                        "message", "删除成功",
                        "data", "null",
                        "error", "null"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "code", 500,
                        "message", "删除失败",
                        "data", "null",
                        "error", "数据库操作未影响任何行"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "删除失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/bucket/{id}")
    public ResponseEntity<Map<String, Object>> getBucketById(@PathVariable Long id) {
        try {
            Bucket bucket = bucketService.getBucketById(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", bucket,
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

    @PutMapping("/bucket/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateBucket(
            @PathVariable Long id,
            @RequestBody Bucket bucket) {
        try {
            Bucket updatedBucket = bucketService.updateBucket(id, bucket);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", updatedBucket,
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
}