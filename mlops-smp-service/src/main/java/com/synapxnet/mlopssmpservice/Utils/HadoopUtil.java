package com.synapxnet.mlopssmpservice.Utils;

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
            if ("kerberos".equalsIgnoreCase(conf.get("hadoop.security.authentication"))) {
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

    private boolean isConnectionActive() {
        if (!isConnected) return false;

        try {
            hdfs.getStatus();
            return true;
        } catch (IOException e) {
            isConnected = false;
            return false;
        }
    }

    public synchronized void closeFileSystem() throws IOException {
        if (hdfs != null) {
            hdfs.close();
            hdfs = null;
            isConnected = false;
        }
    }
}