package com.synapxnet.mlopsmtpservice.controller;

import com.synapxnet.mlopsmtpservice.service.HdfsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/mtp")
public class UploadController {
    @Autowired
    private HdfsService hdfsService;

    // 响应工具类（与DatasetController保持一致）
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

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseUtils.error(400, "请选择上传文件");
        }

        try {
            // 直接上传到HDFS临时目录
            String tempHdfsPath = hdfsService.uploadToTemp(file);
            return ResponseUtils.success("文件上传成功", tempHdfsPath);
        } catch (Exception e) {
            return ResponseUtils.error(500, "文件上传失败", e.getMessage());
        }
    }
}