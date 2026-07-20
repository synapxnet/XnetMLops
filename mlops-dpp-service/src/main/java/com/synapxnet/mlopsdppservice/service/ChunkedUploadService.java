package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.Utils.HadoopUtil;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChunkedUploadService {

    @Autowired
    private HadoopUtil hadoopUtil;

    @Value("${chunked.temp.dir:/tmp/uploads}")
    private String tempDir;

    // 有效期1小时
    private static final long EXPIRATION = 60 * 60 * 1000;

    // 存储上传状态 - 修复了类型声明
    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    public String initUpload(String fileName, long fileSize, int chunkSize) throws IOException {
        String uploadId = UUID.randomUUID().toString();


        // 创建临时目录
        File uploadDir = new File(tempDir, uploadId);
        if (!uploadDir.exists()) {
            if (!uploadDir.mkdirs()) {
                throw new IOException("无法创建临时目录: " + uploadDir.getAbsolutePath());
            }
        }

        // 创建上传会话
        UploadSession session = new UploadSession();
        session.setFileName(fileName);
        session.setFileSize(fileSize);
        session.setChunkSize(chunkSize);
        session.setTotalChunks((int) Math.ceil(fileSize / (double) chunkSize));
        session.setTempDir(uploadDir.getAbsolutePath());
        session.setCreatedAt(System.currentTimeMillis());

        // 存储会话 - 修复了sessions的使用
        sessions.put(uploadId, session);
        return uploadId;
    }

    public void saveChunk(String uploadId, int chunkIndex, byte[] chunkData, int totalChunks)
            throws IOException {

        // 获取会话 - 修复了sessions的使用
        UploadSession session = sessions.get(uploadId);
        if (session == null) {
            throw new IllegalArgumentException("无效的上传ID");
        }

        // 验证分片索引
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new IllegalArgumentException("无效的分片索引: " + chunkIndex +
                    "，总分片数: " + session.getTotalChunks());
        }

        // 保存分片到临时文件
        File chunkFile = new File(session.getTempDir(), "chunk_" + chunkIndex);
        Files.write(chunkFile.toPath(), chunkData);

        // 更新上传状态
        session.getReceivedChunks().add(chunkIndex);
    }

    public String completeUpload(String uploadId, String fileName)
            throws IOException {

        // 获取会话 - 修复了sessions的使用
        UploadSession session = sessions.get(uploadId);
        if (session == null) {
            throw new IllegalArgumentException("无效的上传ID");
        }

        // 检查是否所有分片都已上传
        if (session.getReceivedChunks().size() != session.getTotalChunks()) {
            throw new IllegalStateException("缺少分片文件，已上传: " +
                    session.getReceivedChunks().size() + "/" + session.getTotalChunks());
        }

        // 在HDFS创建目标文件
        String hdfsFilePath = "/temp/" + UUID.randomUUID().toString() + "_" + fileName + ".temp";
        FileSystem fs = null;
        try {
            fs = hadoopUtil.getFileSystem();
        } catch (Exception e) {
            throw new IOException("获取HDFS连接失败: " + e.getMessage(), e);
        }

        try (FSDataOutputStream output = fs.create(new Path(hdfsFilePath))) {

            // 合并所有分片
            for (int i = 0; i < session.getTotalChunks(); i++) {
                File chunkFile = new File(session.getTempDir(), "chunk_" + i);
                byte[] chunkData = Files.readAllBytes(chunkFile.toPath());
                output.write(chunkData);

                // 删除临时分片
                Files.deleteIfExists(chunkFile.toPath());
            }
        } finally {
            // 确保文件系统关闭
            try {
                if (fs != null) {
                    fs.close();
                }
            } catch (IOException e) {
                // 记录错误但继续执行
            }
        }

        // 清理临时目录
        FileUtils.deleteDirectory(new File(session.getTempDir()));

        // 移除会话 - 修复了sessions的使用
        sessions.remove(uploadId);

        return hdfsFilePath;
    }

    public void cancelUpload(String uploadId) throws IOException {
        // 移除会话 - 修复了sessions的使用
        UploadSession session = sessions.remove(uploadId);
        if (session != null) {
            // 递归删除临时目录
            FileUtils.deleteDirectory(new File(session.getTempDir()));
        }
    }

    // 定时清理过期会话
    @Scheduled(fixedRate = 30 * 60 * 1000) // 每30分钟执行一次
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();

        // 修复了sessions.entrySet().removeIf的使用
        sessions.entrySet().removeIf(entry -> {
            UploadSession session = entry.getValue();
            boolean expired = (now - session.getCreatedAt()) > EXPIRATION;
            if (expired) {
                try {
                    FileUtils.deleteDirectory(new File(session.getTempDir()));
                } catch (IOException e) {
                    // 记录错误但继续执行
                }
            }
            return expired;
        });
    }

    // 上传会话类
    @Data
    static class UploadSession {
        private String fileName;
        private long fileSize;
        private int chunkSize;
        private int totalChunks;
        private String tempDir;
        private long createdAt;
        private Set<Integer> receivedChunks = new TreeSet<>();
    }
}