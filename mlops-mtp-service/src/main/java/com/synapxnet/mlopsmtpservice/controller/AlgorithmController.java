package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.entity.Algorithm;
import com.synapxnet.mlopsmtpservice.service.AlgorithmService;
import com.synapxnet.mlopsmtpservice.service.HdfsService;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mtp")
public class AlgorithmController {

    private final AlgorithmService algorithmService;
    private final HdfsService hdfsService;

    @Autowired
    public AlgorithmController(AlgorithmService algorithmService,
                               HdfsService hdfsService) {
        this.algorithmService = algorithmService;
        this.hdfsService = hdfsService;
    }

    // 响应工具类 (复用DatasetController的设计)
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

    @PostMapping("/algorithms-create")
    @Transactional
    public ResponseEntity<Map<String, Object>> createAlgorithm(
            @RequestBody Algorithm request) {
        try {
            Algorithm algorithm = new Algorithm();
            BeanUtils.copyProperties(request, algorithm);
            algorithm.setId(null);
            System.out.println("Received is_CAS: " + request.getIsCAS());
            // 解析原始文件名
            String originalFileName = null;
            if (request.getTempFilePath() != null && !request.getTempFilePath().isEmpty()) {
                String tempFileName = new Path(request.getTempFilePath()).getName();

                // 移除 ".temp" 后缀
                if (tempFileName.endsWith(".temp")) {
                    tempFileName = tempFileName.substring(0, tempFileName.length() - 5);
                }

                // 分割 UUID 和原始文件名
                int underscoreIndex = tempFileName.indexOf('_');
                if (underscoreIndex > 0) {
                    originalFileName = tempFileName.substring(underscoreIndex + 1);
                } else {
                    originalFileName = tempFileName;
                }
            }

            // 先创建算法记录（此时会生成UID）
            Algorithm createdAlgorithm = algorithmService.createAlgorithm(algorithm);

            if (request.getTempFilePath() != null && !request.getTempFilePath().isEmpty()) {
                // 使用创建后得到的UID构建目标路径
                String targetPath = String.format("/algorithms/%s/%s/%s",
                        createdAlgorithm.getBucket_identifier(),
                        createdAlgorithm.getUid(),
                        originalFileName);

                // 处理文件（移动）
                hdfsService.moveAndProcessFile(
                        request.getTempFilePath(), // 源路径字符串
                        targetPath                // 目标路径字符串
                );

                // 如果需要，可以在这里更新算法的存储路径
                // createdAlgorithm.setStoragePath(targetPath);
                // algorithmService.updateAlgorithm(createdAlgorithm.getId(), createdAlgorithm);
            }

            return ResponseUtils.success("算法创建成功", createdAlgorithm);
        } catch (Exception e) {
            return ResponseUtils.error(500, "算法文件处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/algorithms")
    public ResponseEntity<Map<String, Object>> getAllAlgorithms() {
        try {
            List<Algorithm> algorithms = algorithmService.getAllAlgorithms();
            return ResponseUtils.success(algorithms);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取算法列表失败", e.getMessage());
        }
    }

    @GetMapping("/algorithms/{id}")
    public ResponseEntity<Map<String, Object>> getAlgorithmById(@PathVariable Long id) {
        try {
            Algorithm algorithm = algorithmService.getAlgorithmById(id);
            return ResponseUtils.success(algorithm);
        } catch (RuntimeException e) {
            return ResponseUtils.error(404, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取算法详情失败", e.getMessage());
        }
    }

    @PutMapping("/algorithms/{id}")
    public ResponseEntity<Map<String, Object>> updateAlgorithm(
            @PathVariable Long id,
            @RequestBody Algorithm request) {
        try {
            Algorithm existingAlgorithm = algorithmService.getAlgorithmById(id);
            BeanUtils.copyProperties(request, existingAlgorithm);
            existingAlgorithm.setId(id);

            Algorithm updatedAlgorithm = algorithmService.updateAlgorithm(id, existingAlgorithm);
            return ResponseUtils.success("算法更新成功", updatedAlgorithm);
        } catch (RuntimeException e) {
            int code = e.getMessage().contains("已存在") ? 400 : 404;
            return ResponseUtils.error(code, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "更新算法失败", e.getMessage());
        }
    }

    @DeleteMapping("/algorithms/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAlgorithm(@PathVariable Long id) {
        try {
            Algorithm algorithm = algorithmService.getAlgorithmById(id);

            // 删除HDFS中的算法文件 - 与数据集删除逻辑保持一致
            if (algorithm.getUid() != null && !algorithm.getUid().isEmpty()) {
                // 构建正确的删除路径: /algorithms/bucket_identifier/uid
                String pathToDelete = String.format("/algorithms/%s/%s",
                        algorithm.getBucket_identifier(),
                        algorithm.getUid());

                hdfsService.deleteDirectory(pathToDelete);
            }

            // 删除数据库记录 - 与数据集删除返回逻辑保持一致
            algorithmService.deleteAlgorithm(id);
            return ResponseUtils.success("算法删除成功", "null");
        } catch (RuntimeException e) {
            return ResponseUtils.error(404, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "删除算法失败", e.getMessage());
        }
    }
}