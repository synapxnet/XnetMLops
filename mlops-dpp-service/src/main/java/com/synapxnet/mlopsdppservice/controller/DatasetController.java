package com.synapxnet.mlopsdppservice.controller;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import com.synapxnet.mlopsdppservice.service.DatasetService;
import com.synapxnet.mlopsdppservice.service.HdfsService;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dpp")
public class DatasetController {

    private final DatasetService datasetService;
    private final HdfsService hdfsService; // 添加HdfsService

    @Autowired
    public DatasetController(DatasetService datasetService,
                             HdfsService hdfsService) {
        this.datasetService = datasetService;
        this.hdfsService = hdfsService; // 注入HdfsService
    }

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

    @PostMapping("/datasets-create")
    @Transactional
    public ResponseEntity<Map<String, Object>> createDataset(
            @RequestBody Dataset request) {
        try {
            Dataset dataset = new Dataset();
            BeanUtils.copyProperties(request, dataset);
            dataset.setId(null);

            // 解析原始文件名
            String originalFileName = null;
            String fileExtension = null; // 添加文件扩展名变量
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

                // 获取文件扩展名
                int dotIndex = originalFileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileExtension = originalFileName.substring(dotIndex + 1).toLowerCase();
                }
            }

            Dataset createdDataset = datasetService.createDataset(dataset);

            if (request.getTempFilePath() != null && !request.getTempFilePath().isEmpty()) {
                // 使用原始文件名构建目标路径
                String targetPath = String.format("%s/%s/%s/%s",
                        "/datasets",
                        createdDataset.getBucket_identifier(),
                        createdDataset.getUid(),
                        originalFileName); // 直接使用文件名

                // 处理文件（移动并解压）
                hdfsService.moveAndProcessFile(
                        request.getTempFilePath(), // 源路径字符串
                        targetPath                // 目标路径字符串
                );

                // 检查是否为压缩文件并更新存储路径
                String finalStoragePath = targetPath;
                if ("zip".equalsIgnoreCase(fileExtension)) {
                    // 解压后的路径是去掉扩展名的目录
                    finalStoragePath = targetPath.substring(0, targetPath.lastIndexOf('.'));
                }

            }

            return ResponseUtils.success("数据集创建成功", createdDataset);
        } catch (Exception e) {
            return ResponseUtils.error(500, "文件处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/datasets")
    public ResponseEntity<Map<String, Object>> getAllDatasets() {
        try {
            List<Dataset> datasets = datasetService.getAllDatasets();
            return ResponseUtils.success(datasets);
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取数据集列表失败", e.getMessage());
        }
    }

    @GetMapping("/datasets/{id}")
    public ResponseEntity<Map<String, Object>> getDatasetById(@PathVariable("id") Long id) {
        try {
            Dataset dataset = datasetService.getDatasetById(id);
            return ResponseUtils.success(dataset);
        } catch (RuntimeException e) {
            return ResponseUtils.error(404, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "获取数据集失败", e.getMessage());
        }
    }

    @PutMapping("/datasets/{id}")
    public ResponseEntity<Map<String, Object>> updateDataset(
            @PathVariable("id") Long id,
            @RequestBody Dataset request) {
        try {
            Dataset existingDataset = datasetService.getDatasetById(id);
            BeanUtils.copyProperties(request, existingDataset);
            existingDataset.setId(id);

            Dataset updatedDataset = datasetService.updateDataset(id, existingDataset);
            return ResponseUtils.success("数据集更新成功", updatedDataset);
        } catch (RuntimeException e) {
            // 统一处理名称重复和不存在异常
            int code = e.getMessage().contains("数据集名称已存在") ? 400 : 404;
            return ResponseUtils.error(code, e.getMessage());
        } catch (Exception e) {
            return ResponseUtils.error(500, "更新数据集失败", e.getMessage());
        }
    }

    @DeleteMapping("/datasets/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteDataset(@PathVariable("id") Long id) {
        try {
            Dataset dataset = datasetService.getDatasetById(id);

            // 删除HDFS中的文件 - 修改路径
            if (dataset.getUid() != null && !dataset.getUid().isEmpty()) {
                // 构建正确的删除路径: /datasets/bucket_identifier/uid
                String pathToDelete = String.format("/datasets/%s/%s",
                        dataset.getBucket_identifier(),
                        dataset.getUid());

                hdfsService.deleteDirectory(pathToDelete);
            }

            // 删除数据库记录
            boolean success = datasetService.deleteDataset(id);
            if (success) {
                return ResponseUtils.success("数据集删除成功", "null");
            } else {
                return ResponseUtils.error(500, "删除数据集失败");
            }
        } catch (Exception e) {
            return ResponseUtils.error(500, "删除失败: " + e.getMessage());
        }
    }
}
