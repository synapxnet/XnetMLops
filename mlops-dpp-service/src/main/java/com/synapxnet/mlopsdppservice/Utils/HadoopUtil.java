package com.synapxnet.mlopsdppservice.Utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.security.PrivilegedExceptionAction;

@Component
public class HadoopUtil {
    @Value("${hdfs.path}")
    private String path;

    @Value("${hdfs.user}")
    private String username;

    private volatile FileSystem hdfs;
    private volatile boolean isConnected = false;

    public synchronized FileSystem getFileSystem() throws Exception {
        if (hdfs == null || !isConnectionActive()) {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", path);
            conf.set("dfs.client.use.datanode.hostname", "true");

            // Kerberos 支持 (可选)
            String authType = conf.get("hadoop.security.authentication");
            if (authType != null && "kerberos".equalsIgnoreCase(authType)) {
                UserGroupInformation.setConfiguration(conf);
                UserGroupInformation.loginUserFromKeytab(
                        conf.get("kerberos.principal"),
                        conf.get("kerberos.keytab")
                );
            }

            hdfs = UserGroupInformation
                    .createRemoteUser(username)
                    .doAs((PrivilegedExceptionAction<FileSystem>) () ->
                            FileSystem.get(new URI(path), conf)
                    );

            isConnected = true;
        }
        return hdfs;
    }

    /**
     * 检查HDFS连接是否活跃
     */
    private boolean isConnectionActive() {
        if (!isConnected || hdfs == null) return false;

        try {
            // 通过执行简单操作检查连接状态
            hdfs.getStatus();
            return true;
        } catch (IOException e) {
            // 连接已断开
            isConnected = false;
            hdfs = null; // 重置连接
            return false;
        }
    }

    public synchronized void closeFileSystem() {
        if (hdfs != null) {
            try {
                hdfs.close();
            } catch (IOException e) {
                // 忽略关闭错误
            }
            hdfs = null;
            isConnected = false;
        }
    }

    /**
     * 重试获取文件系统
     */
    public FileSystem getFileSystemWithRetry() throws Exception {
        int retryCount = 0;
        final int maxRetries = 3;

        while (retryCount < maxRetries) {
            try {
                return getFileSystem();
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e;
                }
                Thread.sleep(1000 * retryCount); // 指数退避
            }
        }
        return null;
    }
}