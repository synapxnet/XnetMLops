package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.Utils.HadoopUtil;

import com.synapxnet.mlopsdppservice.entity.HdfsFile;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DatasetManagerService {

    private static final Logger logger = LoggerFactory.getLogger(DatasetManagerService.class);

    private final HadoopUtil hadoopUtil;

    @Value("${app.temp.dir:#{null}}")
    private String tempDir;

    @Autowired
    public DatasetManagerService(HadoopUtil hadoopUtil) {
        this.hadoopUtil = hadoopUtil;
    }

    public List<HdfsFile> listHdfsFiles(String path) throws Exception {
        List<HdfsFile> fileList = new ArrayList<>();
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            // 使用 Hadoop Path 而不是 Java NIO Path
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

    public void uploadFileToHdfs(MultipartFile file, String remoteFilePath) throws Exception {
        // 获取系统临时目录
        String actualTempDir = (tempDir != null && !tempDir.isEmpty())
                ? tempDir
                : System.getProperty("java.io.tmpdir");

        // 确保临时目录存在
        File tempDirFile = new File(actualTempDir);
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }

        // 创建临时文件路径 - 使用 File.separator 确保跨平台兼容
        String tempFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        java.nio.file.Path tempFilePath = Paths.get(actualTempDir, tempFilename);

        logger.info("上传文件到HDFS: 临时文件路径={}, HDFS路径={}", tempFilePath, remoteFilePath);

        try (InputStream inputStream = file.getInputStream()) {
            // 将文件保存到临时位置
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("文件已保存到临时位置: {}", tempFilePath);

            try (FileSystem fs = hadoopUtil.getFileSystem()) {
                // 使用 Hadoop Path
                Path hdfsPath = new Path(remoteFilePath);
                if (fs.exists(hdfsPath)) {
                    fs.delete(hdfsPath, false);
                }

                // 使用 Hadoop 方法上传文件 - 转换为绝对路径字符串
                fs.copyFromLocalFile(
                        new Path(tempFilePath.toAbsolutePath().toString()), // 源路径 (本地)
                        hdfsPath              // 目标路径 (HDFS)
                );
                logger.info("文件已上传到HDFS: {}", hdfsPath);
            }
        } finally {
            // 删除临时文件
            try {
                Files.deleteIfExists(tempFilePath);
                logger.info("临时文件已删除: {}", tempFilePath);
            } catch (Exception e) {
                logger.warn("删除临时文件失败: {}", e.getMessage());
            }
        }
    }

    public ByteArrayOutputStream downloadFileFromHdfs(String filePath) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (FileSystem fs = hadoopUtil.getFileSystem();
             FSDataInputStream inputStream = fs.open(new Path(filePath))) {
            IOUtils.copyBytes(inputStream, outputStream, 4096, false);
        }
        return outputStream;
    }

    public void deleteFromHdfs(String path) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            fs.delete(new Path(path), true);
        }
    }

    public void createHdfsDirectory(String path) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            Path hdfsPath = new Path(path);
            if (!fs.exists(hdfsPath)) {
                fs.mkdirs(hdfsPath);
            }
        }
    }
}
