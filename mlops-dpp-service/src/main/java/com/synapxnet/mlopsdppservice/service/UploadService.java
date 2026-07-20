package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.Utils.HadoopUtil;
import org.apache.hadoop.fs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class UploadService {
    @Autowired
    private HadoopUtil hadoopUtil;

    @Value("${hdfs.upload.retry:3}")
    private int maxRetries;

    public boolean uploadFileToHdfs(String localFilePath, String remoteFilePath) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try (FileSystem fs = hadoopUtil.getFileSystem();
                 InputStream inputStream = Files.newInputStream(Paths.get(localFilePath))) {

                Path hdfsPath = new Path(remoteFilePath);
                if (fs.exists(hdfsPath)) {
                    fs.delete(hdfsPath, false); // 删除已存在文件
                }

                try (FSDataOutputStream outputStream = fs.create(hdfsPath)) {
                    byte[] buffer = new byte[1024 * 1024]; // 1MB缓冲区
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                return true;
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    e.printStackTrace();
                    return false;
                }
                try {
                    Thread.sleep(1000 * attempt); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return false;
    }
}