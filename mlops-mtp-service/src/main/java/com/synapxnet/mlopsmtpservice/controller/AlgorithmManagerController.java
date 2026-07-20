package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.entity.Algorithm;
import com.synapxnet.mlopsmtpservice.entity.HdfsFile;
import com.synapxnet.mlopsmtpservice.service.AlgorithmService;
import com.synapxnet.mlopsmtpservice.Utils.HadoopUtil;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mtp/algorithms/{algorithmId}")
public class AlgorithmManagerController {

    private static final Logger logger = LoggerFactory.getLogger(AlgorithmManagerController.class);

    private final AlgorithmService algorithmService;
    private final HadoopUtil hadoopUtil;

    @Autowired
    public AlgorithmManagerController(AlgorithmService algorithmService, HadoopUtil hadoopUtil) {
        this.algorithmService = algorithmService;
        this.hadoopUtil = hadoopUtil;
    }

    // 辅助方法：根据algorithmId构建算法根路径
    private String buildAlgorithmRootPath(Long algorithmId) {
        Algorithm algorithm = algorithmService.getAlgorithmById(algorithmId);
        return String.format("/algorithms/%s/%s",
                algorithm.getBucket_identifier(),
                algorithm.getUid()
        );
    }

    // 辅助方法：检查算法是否为CAS模式
    private boolean isCasMode(Long algorithmId) {
        Algorithm algorithm = algorithmService.getAlgorithmById(algorithmId);
        return algorithm.getIsCAS() != null && algorithm.getIsCAS();
    }

    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> listFiles(
            @PathVariable("algorithmId") Long algorithmId,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path) {
        logger.info("listFiles 请求: algorithmId={}, path={}", algorithmId, path);
        try {
            String algorithmRootPath = buildAlgorithmRootPath(algorithmId);
            logger.info("算法根路径: {}", algorithmRootPath);

            String fullPath = path.startsWith("/")
                    ? algorithmRootPath + path
                    : algorithmRootPath + "/" + path;

            // 如果传入的路径已经包含算法根路径，则直接使用
            if (path.startsWith(algorithmRootPath)) {
                fullPath = path;
            }

            logger.info("完整HDFS路径: {}", fullPath);

            List<HdfsFile> files = listHdfsFiles(fullPath);
            logger.info("获取到 {} 个文件", files.size());

            // 获取CAS状态
            boolean isCas = isCasMode(algorithmId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("message", "Success");
            response.put("data", files);
            response.put("isCAS", isCas);
            response.put("error", "null");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("RuntimeException: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", HttpStatus.NOT_FOUND.value());
            response.put("message", "Algorithm not found");
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
            @PathVariable("algorithmId") Long algorithmId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path) {
        try {
            // 检查是否为CAS模式
            if (isCasMode(algorithmId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "code", HttpStatus.FORBIDDEN.value(),
                        "message", "CAS模式下不允许上传文件，请使用云算法仓库",
                        "data", "null",
                        "error", "CAS mode does not allow file upload"
                ));
            }

            String algorithmRootPath = buildAlgorithmRootPath(algorithmId);
            String fullPath = path.startsWith("/")
                    ? algorithmRootPath + path
                    : algorithmRootPath + "/" + path;

            String hdfsPath = fullPath + "/" + file.getOriginalFilename();

            // 上传文件（覆盖模式）
            uploadFileToHdfs(file, hdfsPath);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "File uploaded successfully",
                    "data", Map.of("filePath", hdfsPath),
                    "error", "null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", HttpStatus.NOT_FOUND.value(),
                    "message", "Algorithm not found",
                    "data", "null",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Failed to upload file",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable("algorithmId") Long algorithmId,
            @RequestParam("filePath") String filePath) {
        try {
            String algorithmRootPath = buildAlgorithmRootPath(algorithmId);

            // 如果传入的路径已经包含算法根路径，则直接使用
            String fullPath;
            if (filePath.startsWith(algorithmRootPath)) {
                fullPath = filePath;
            } else if (filePath.startsWith("/")) {
                fullPath = algorithmRootPath + filePath;
            } else {
                fullPath = algorithmRootPath + "/" + filePath;
            }

            logger.info("下载文件: 传入路径={}, 完整路径={}", filePath, fullPath);

            ByteArrayOutputStream outputStream = downloadFileFromHdfs(fullPath);
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
            @PathVariable("algorithmId") Long algorithmId,
            @RequestBody Map<String, String> request) {
        try {
            // 检查是否为CAS模式
            if (isCasMode(algorithmId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "code", HttpStatus.FORBIDDEN.value(),
                        "message", "CAS模式下不允许删除文件",
                        "data", "null",
                        "error", "CAS mode does not allow file deletion"
                ));
            }

            String path = request.get("path");
            String algorithmRootPath = buildAlgorithmRootPath(algorithmId);

            // 如果传入的路径已经包含算法根路径，则直接使用
            String fullPath;
            if (path.startsWith(algorithmRootPath)) {
                fullPath = path;
            } else if (path.startsWith("/")) {
                fullPath = algorithmRootPath + path;
            } else {
                fullPath = algorithmRootPath + "/" + path;
            }

            logger.info("删除文件: 传入路径={}, 算法根路径={}, 完整路径={}", path, algorithmRootPath, fullPath);

            deleteFromHdfs(fullPath);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "File deleted successfully",
                    "data", "null",
                    "error", "null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", HttpStatus.NOT_FOUND.value(),
                    "message", "Algorithm not found",
                    "data", "null",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Failed to delete file",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/mkdir")
    public ResponseEntity<Map<String, Object>> createDirectory(
            @PathVariable("algorithmId") Long algorithmId,
            @RequestBody Map<String, String> request) {
        try {
            // 检查是否为CAS模式
            if (isCasMode(algorithmId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "code", HttpStatus.FORBIDDEN.value(),
                        "message", "CAS模式下不允许创建目录",
                        "data", "null",
                        "error", "CAS mode does not allow directory creation"
                ));
            }

            String path = request.get("path");
            String folderName = request.get("folderName");
            String algorithmRootPath = buildAlgorithmRootPath(algorithmId);
            String fullPath = path.startsWith("/")
                    ? algorithmRootPath + path
                    : algorithmRootPath + "/" + path;

            String hdfsPath = fullPath + "/" + folderName;
            createHdfsDirectory(hdfsPath);

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "Directory created successfully",
                    "data", "null",
                    "error", "null"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "code", HttpStatus.NOT_FOUND.value(),
                    "message", "Algorithm not found",
                    "data", "null",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Failed to create directory",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // HDFS操作方法
    private List<HdfsFile> listHdfsFiles(String path) throws Exception {
        List<HdfsFile> fileList = new ArrayList<>();
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            Path hdfsPath = new Path(path);

            if (!fs.exists(hdfsPath)) {
                return fileList;
            }

            FileStatus[] statuses = fs.listStatus(hdfsPath);
            for (FileStatus status : statuses) {
                HdfsFile file = new HdfsFile();
                file.setId(status.getPath().toString());
                file.setName(status.getPath().getName());
                file.setPath(status.getPath().toString());
                file.setDirectory(status.isDirectory());
                file.setSize(status.getLen());
                file.setModificationTime(status.getModificationTime());
                file.setPermissions(status.getPermission().toString());
                file.setOwner(status.getOwner());
                file.setGroup(status.getGroup());
                fileList.add(file);
            }
        }
        return fileList;
    }

    private void uploadFileToHdfs(MultipartFile file, String remoteFilePath) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem();
             InputStream inputStream = file.getInputStream()) {

            Path hdfsPath = new Path(remoteFilePath);

            // 确保父目录存在
            Path parentDir = hdfsPath.getParent();
            if (!fs.exists(parentDir)) {
                fs.mkdirs(parentDir);
            }

            // 如果文件存在则删除（覆盖模式）
            if (fs.exists(hdfsPath)) {
                fs.delete(hdfsPath, false);
            }

            try (FSDataOutputStream outputStream = fs.create(hdfsPath)) {
                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            logger.info("文件已上传到HDFS: {}", hdfsPath);
        }
    }

    private ByteArrayOutputStream downloadFileFromHdfs(String filePath) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (FileSystem fs = hadoopUtil.getFileSystem();
             FSDataInputStream inputStream = fs.open(new Path(filePath))) {
            IOUtils.copyBytes(inputStream, outputStream, 4096, false);
        }
        return outputStream;
    }

    private void deleteFromHdfs(String path) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            fs.delete(new Path(path), true);
        }
    }

    private void createHdfsDirectory(String path) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            Path hdfsPath = new Path(path);
            if (!fs.exists(hdfsPath)) {
                fs.mkdirs(hdfsPath);
            }
        }
    }
}
