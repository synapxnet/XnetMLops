package com.synapxnet.mlopsmepservice.service;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * SSH远程执行服务
 * 通过SMP获取工作站凭证，通过JSch在远程服务器执行命令
 */
@Service
@Slf4j
public class SshRemoteService {

    private final WebClient smpClient;

    public SshRemoteService(WebClient.Builder webClientBuilder,
                            @Value("${smp.service.url}") String smpServiceUrl) {
        this.smpClient = webClientBuilder.baseUrl(smpServiceUrl).build();
    }

    /**
     * 从SMP获取工作站SSH凭证（解密后）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchCredentials(Long workstationId) {
        try {
            Map<String, Object> response = smpClient.get()
                    .uri("/api/smp/workstations/{id}/credentials", workstationId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Integer.valueOf(0).equals(response.get("code"))) {
                return (Map<String, Object>) response.get("data");
            }
            throw new RuntimeException("获取工作站凭证失败: " + (response != null ? response.get("message") : "无响应"));
        } catch (Exception e) {
            log.error("获取工作站 {} 凭证失败: {}", workstationId, e.getMessage());
            throw new RuntimeException("获取工作站凭证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建SSH Session
     */
    public Session createSession(Map<String, Object> credentials) throws JSchException {
        String host = (String) credentials.get("ipAddress");
        int port = credentials.get("sshPort") != null ? ((Number) credentials.get("sshPort")).intValue() : 22;
        String user = (String) credentials.get("sshUser");
        String authType = (String) credentials.get("authType");
        String password = (String) credentials.get("password");
        String privateKey = (String) credentials.get("privateKey");

        JSch jsch = new JSch();

        if ("privateKey".equals(authType) && privateKey != null && !privateKey.isEmpty()) {
            jsch.addIdentity("workstation-key", privateKey.getBytes(StandardCharsets.UTF_8), null, null);
        }

        Session session = jsch.getSession(user, host, port);

        if ("password".equals(authType) && password != null) {
            session.setPassword(password);
        }

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey,password");
        session.setConfig(config);
        session.setTimeout(30000);
        session.connect();

        log.info("SSH连接成功: {}@{}:{}", user, host, port);
        return session;
    }

    /**
     * 在远程服务器执行命令（默认超时）
     */
    public String executeCommand(Map<String, Object> credentials, String command) {
        return executeCommand(credentials, command, 30000);
    }

    /**
     * 在远程服务器执行命令（自定义超时）
     * @param timeoutMs session 超时毫秒数，0表示不限制
     */
    public String executeCommand(Map<String, Object> credentials, String command, int timeoutMs) {
        Session session = null;
        ChannelExec channel = null;
        try {
            session = createSession(credentials);
            if (timeoutMs > 0) {
                session.setTimeout(timeoutMs);
            } else {
                session.setTimeout(0); // 不限制
            }
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            InputStream inputStream = channel.getInputStream();
            InputStream errStream = channel.getErrStream();
            channel.connect(timeoutMs > 0 ? timeoutMs : 0);

            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 读取错误流
            BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8));
            StringBuilder errOutput = new StringBuilder();
            while ((line = errReader.readLine()) != null) {
                errOutput.append(line).append("\n");
            }

            // 等待命令完成
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            int exitStatus = channel.getExitStatus();
            String result = output.toString().trim();

            if (exitStatus != 0 && result.isEmpty()) {
                log.warn("远程命令退出码 {}: {}", exitStatus, errOutput.toString().trim());
            }

            log.debug("远程命令执行完成, exitCode={}, output={}", exitStatus, result);
            return result;
        } catch (Exception e) {
            log.error("远程命令执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("远程命令执行失败: " + e.getMessage(), e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 通过SFTP上传文件内容到远程服务器
     */
    public void uploadFile(Map<String, Object> credentials, byte[] content, String remotePath) {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            session = createSession(credentials);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            // 确保远程目录存在
            String remoteDir = remotePath.substring(0, remotePath.lastIndexOf('/'));
            mkdirs(sftp, remoteDir);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                sftp.put(bais, remotePath);
            }

            log.info("文件上传成功: {}", remotePath);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 递归创建远程目录
     */
    private void mkdirs(ChannelSftp sftp, String path) throws SftpException {
        String[] dirs = path.split("/");
        StringBuilder currentPath = new StringBuilder();
        for (String dir : dirs) {
            if (dir.isEmpty()) {
                currentPath.append("/");
                continue;
            }
            currentPath.append(dir).append("/");
            try {
                sftp.stat(currentPath.toString());
            } catch (SftpException e) {
                sftp.mkdir(currentPath.toString());
            }
        }
    }
}
