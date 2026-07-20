package com.synapxnet.mlopsmtpservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface HdfsService {
    /**
     * 上传文件到HDFS临时目录（添加.temp后缀）
     * @param file 上传的文件
     * @return 临时文件路径 (e.g. /temp/abc123.zip.temp)
     */
    String uploadToTemp(MultipartFile file) throws Exception;

    /**
     * 移动并处理文件
     * @param tempPath 临时文件路径 (带.temp后缀)
     * @param targetPath 目标路径 (e.g. /processed/final_file)
     */
    void moveAndProcessFile(String tempPath, String targetPath) throws Exception;

    /**
     * 删除目录
     * @param path 要删除的目录路径
     */
    void deleteDirectory(String path) throws Exception;

    /**
     * 获取存储桶路径
     * @param uid 用户ID
     */
    String getBucketPath(String uid);



}
