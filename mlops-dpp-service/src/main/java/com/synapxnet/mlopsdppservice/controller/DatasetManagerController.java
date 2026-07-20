package com.synapxnet.mlopsdppservice.controller;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import com.synapxnet.mlopsdppservice.service.DatasetManagerService;
import com.synapxnet.mlopsdppservice.entity.HdfsFile;
import com.synapxnet.mlopsdppservice.Utils.HadoopUtil;
import com.synapxnet.mlopsdppservice.service.DatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dpp/datasets/{datasetId}")
public class DatasetManagerController {

    private static final Logger logger = LoggerFactory.getLogger(DatasetManagerController.class);

    private final DatasetManagerService datasetManagerService;
    private final HadoopUtil hadoopUtil;
    private final DatasetService datasetService;

    @Autowired
    public DatasetManagerController(DatasetManagerService datasetManagerService,
                                    HadoopUtil hadoopUtil,
                                    DatasetService datasetService) {
        this.datasetManagerService = datasetManagerService;
        this.hadoopUtil = hadoopUtil;
        this.datasetService = datasetService;
    }

    // 辅助方法：根据datasetId构建数据集根路径
    private String buildDatasetRootPath(Long datasetId) {
        Dataset dataset = datasetService.getDatasetById(datasetId);
        return String.format("/datasets/%s/%s/%s",
                dataset.getBucket_identifier(),
                dataset.getUid(),
                dataset.getDataset_file()
        );
    }

    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> listFiles(
            @PathVariable("datasetId") Long datasetId,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path) {
        logger.info("listFiles 请求: datasetId={}, path={}", datasetId, path);
        try {
            String datasetRootPath = buildDatasetRootPath(datasetId);
            logger.info("数据集根路径: {}", datasetRootPath);

            String fullPath = path.startsWith("/")
                    ? datasetRootPath + path
                    : datasetRootPath + "/" + path;
            logger.info("完整HDFS路径: {}", fullPath);

            List<HdfsFile> files = datasetManagerService.listHdfsFiles(fullPath);
            logger.info("获取到 {} 个文件", files.size());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("message", "Success");
            response.put("data", files);
            response.put("error", "null");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("RuntimeException: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", HttpStatus.NOT_FOUND.value());
            response.put("message", "Dataset not found");
            response.put("data", "null");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("Exception: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message", "Failed to list files");
            response.put("data", "null");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable("datasetId") Long datasetId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path) {
        try {
            String datasetRootPath = buildDatasetRootPath(datasetId);
            String fullPath = path.startsWith("/")
                    ? datasetRootPath + path
                    : datasetRootPath + "/" + path;

            String hdfsPath = fullPath + "/" + file.getOriginalFilename();
            datasetManagerService.uploadFileToHdfs(file, hdfsPath);

            return ResponseEntity.ok(Map.of(
                    "code", 0,  // 成功码改为0
                    "message", "File uploaded successfully",
                    "data", Map.of("filePath", hdfsPath),
                    "error", "null"  // 字符串"null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", HttpStatus.NOT_FOUND.value(),
                    "message", "Dataset not found",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Failed to upload file",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable("datasetId") Long datasetId,
            @RequestParam("filePath") String filePath) {
        try {
            String datasetRootPath = buildDatasetRootPath(datasetId);

            // 如果传入的路径已经包含数据集根路径，则直接使用；否则拼接
            String fullPath;
            if (filePath.startsWith(datasetRootPath)) {
                fullPath = filePath;
            } else if (filePath.startsWith("/")) {
                fullPath = datasetRootPath + filePath;
            } else {
                fullPath = datasetRootPath + "/" + filePath;
            }

            logger.info("下载文件: 传入路径={}, 完整路径={}", filePath, fullPath);

            ByteArrayOutputStream outputStream = datasetManagerService.downloadFileFromHdfs(fullPath);
            byte[] data = outputStream.toByteArray();
            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(data));

            String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable("datasetId") Long datasetId,
            @RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");
            String datasetRootPath = buildDatasetRootPath(datasetId);

            // 如果传入的路径已经包含数据集根路径，则直接使用；否则拼接
            String fullPath;
            if (path.startsWith(datasetRootPath)) {
                fullPath = path;
            } else if (path.startsWith("/")) {
                fullPath = datasetRootPath + path;
            } else {
                fullPath = datasetRootPath + "/" + path;
            }

            logger.info("删除文件: 传入路径={}, 数据集根路径={}, 完整路径={}", path, datasetRootPath, fullPath);

            datasetManagerService.deleteFromHdfs(fullPath);

            return ResponseEntity.ok(Map.of(
                    "code", 0,  // 成功码改为0
                    "message", "File deleted successfully",
                    "data", "null",  // 字符串"null"
                    "error", "null"  // 字符串"null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", HttpStatus.NOT_FOUND.value(),
                    "message", "Dataset not found",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Failed to delete file",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/batchDelete")
    public ResponseEntity<Map<String, Object>> batchDelete(
            @PathVariable("datasetId") Long datasetId,
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> paths = request.get("paths");
            String datasetRootPath = buildDatasetRootPath(datasetId);

            for (String path : paths) {
                // 如果传入的路径已经包含数据集根路径，则直接使用；否则拼接
                String fullPath;
                if (path.startsWith(datasetRootPath)) {
                    fullPath = path;
                } else if (path.startsWith("/")) {
                    fullPath = datasetRootPath + path;
                } else {
                    fullPath = datasetRootPath + "/" + path;
                }
                logger.info("批量删除文件: 传入路径={}, 完整路径={}", path, fullPath);
                datasetManagerService.deleteFromHdfs(fullPath);
            }

            return ResponseEntity.ok(Map.of(
                    "code", 0,  // 成功码改为0
                    "message", "Files deleted successfully",
                    "data", "null",  // 字符串"null"
                    "error", "null"  // 字符串"null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", HttpStatus.NOT_FOUND.value(),
                    "message", "Dataset not found",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Failed to delete files",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/mkdir")
    public ResponseEntity<Map<String, Object>> createDirectory(
            @PathVariable("datasetId") Long datasetId,
            @RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");
            String folderName = request.get("folderName");
            String datasetRootPath = buildDatasetRootPath(datasetId);
            String fullPath = path.startsWith("/")
                    ? datasetRootPath + path
                    : datasetRootPath + "/" + path;

            String hdfsPath = fullPath + "/" + folderName;
            datasetManagerService.createHdfsDirectory(hdfsPath);

            return ResponseEntity.ok(Map.of(
                    "code", 0,  // 成功码改为0
                    "message", "Directory created successfully",
                    "data", "null",  // 字符串"null"
                    "error", "null"  // 字符串"null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", HttpStatus.NOT_FOUND.value(),
                    "message", "Dataset not found",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Failed to create directory",
                    "data", "null",  // 字符串"null"
                    "error", e.getMessage()
            ));
        }
    }
}
