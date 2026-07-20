package com.synapxnet.mlopsmtpservice.service;

import com.synapxnet.mlopsmtpservice.Utils.HadoopUtil;
import com.synapxnet.mlopsmtpservice.service.HdfsService;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class HdfsServiceImpl implements HdfsService {

    @Autowired
    private HadoopUtil hadoopUtil;

    @Value("${hdfs.upload.retry:3}")
    private int maxRetries;

    @Override
    public String uploadToTemp(MultipartFile file) throws Exception {
        String tempFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename() + ".temp";
        String hdfsTempPath = "/temp/" + tempFileName;

        try (InputStream inputStream = file.getInputStream();
             FileSystem fs = hadoopUtil.getFileSystem();
             FSDataOutputStream outputStream = fs.create(new Path(hdfsTempPath))) {

            byte[] buffer = new byte[1024 * 1024]; // 1MB缓冲区
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return hdfsTempPath;
    }

    @Override
    public void moveAndProcessFile(String tempPath, String targetPath) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            Path src = new Path(tempPath);
            Path dest = new Path(targetPath);

            // 确保目标目录存在
            Path parentDir = dest.getParent();
            if (!fs.exists(parentDir)) {
                fs.mkdirs(parentDir);
            }

            // 重命名文件（移动）
            boolean success = fs.rename(src, dest);
            if (!success) {
                throw new IOException("文件移动失败: " + tempPath + " -> " + targetPath);
            }

            // 如果是ZIP文件则解压
            if (dest.getName().toLowerCase().endsWith(".zip")) {
                // 创建解压目录 (使用文件名作为目录名)
                String dirName = dest.getName().replace(".zip", "");
                Path unzipDir = new Path(parentDir, dirName);

                // 调用解压方法
                unzipFile(fs, dest, unzipDir);

                // 删除原始ZIP文件
                fs.delete(dest, false);
            }
        }
    }

    // 私有解压方法（不需要在接口中声明）
    private void unzipFile(FileSystem fs, Path zipPath, Path targetDir) throws IOException {
        try (FSDataInputStream fis = fs.open(zipPath);
             ZipArchiveInputStream zis = new ZipArchiveInputStream(fis)) {

            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (entry.isDirectory()) continue;

                Path entryPath = new Path(targetDir, entry.getName());
                // 确保父目录存在
                Path parentDir = entryPath.getParent();
                if (!fs.exists(parentDir)) {
                    fs.mkdirs(parentDir);
                }

                try (FSDataOutputStream fos = fs.create(entryPath)) {
                    byte[] buffer = new byte[1024 * 1024]; // 1MB缓冲区
                    int bytesRead;
                    while ((bytesRead = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    @Override
    public void deleteDirectory(String path) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            Path dirPath = new Path(path);
            if (fs.exists(dirPath)) {
                fs.delete(dirPath, true);
            }
        }
    }

    @Override
    public String getBucketPath(String uid) {
        return "/buckets/" + uid;
    }
}