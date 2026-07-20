package com.synapxnet.mlopssmpservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import com.synapxnet.mlopssmpservice.entity.Workstation;
import com.synapxnet.mlopssmpservice.mapper.WorkstationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作站点服务实现
 */
@Service
public class WorkstationServiceImpl implements WorkstationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkstationServiceImpl.class);
    private static final String AES_KEY = "SmpWorkstation16"; // 16字节密钥

    @Autowired
    private WorkstationMapper workstationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Workstation> getAll() {
        return workstationMapper.findAll();
    }

    @Override
    public Workstation getById(Long id) {
        return workstationMapper.findById(id)
                .orElseThrow(() -> new RuntimeException("工作站点不存在: " + id));
    }

    @Override
    public Workstation getByUid(String uid) {
        return workstationMapper.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("工作站点不存在: " + uid));
    }

    @Override
    @Transactional
    public Workstation create(Workstation workstation, String userId) {
        // 检查名称是否已存在
        if (isNameExists(workstation.getName())) {
            throw new RuntimeException("服务器名称已存在: " + workstation.getName());
        }

        // 生成 UID
        workstation.setUid(UUID.randomUUID().toString());
        workstation.setCreatedBy(userId);
        workstation.setCreatedAt(LocalDateTime.now());
        workstation.setUpdatedAt(LocalDateTime.now());
        workstation.setStatus("pending");

        // 加密密码和私钥
        if (workstation.getPassword() != null && !workstation.getPassword().isEmpty()) {
            workstation.setEncryptedPassword(encrypt(workstation.getPassword()));
        }
        if (workstation.getPrivateKey() != null && !workstation.getPrivateKey().isEmpty()) {
            workstation.setEncryptedPrivateKey(encrypt(workstation.getPrivateKey()));
        }

        // 设置默认值
        if (workstation.getSshPort() == null) {
            workstation.setSshPort(22);
        }
        if (workstation.getCpuCores() == null) {
            workstation.setCpuCores(1);
        }
        if (workstation.getRamGb() == null) {
            workstation.setRamGb(1);
        }
        if (workstation.getDiskGb() == null) {
            workstation.setDiskGb(10);
        }

        workstationMapper.insert(workstation);
        logger.info("创建工作站点: {} ({})", workstation.getName(), workstation.getIpAddress());
        return workstation;
    }

    @Override
    @Transactional
    public Workstation update(Long id, Workstation workstation, String userId) {
        Workstation existing = getById(id);

        // 如果修改了名称，检查是否冲突
        if (!existing.getName().equals(workstation.getName()) && isNameExists(workstation.getName())) {
            throw new RuntimeException("服务器名称已存在: " + workstation.getName());
        }

        workstation.setId(id);
        workstation.setUid(existing.getUid());
        workstation.setUpdatedAt(LocalDateTime.now());

        // 加密密码和私钥
        if (workstation.getPassword() != null && !workstation.getPassword().isEmpty()) {
            workstation.setEncryptedPassword(encrypt(workstation.getPassword()));
        } else {
            workstation.setEncryptedPassword(existing.getEncryptedPassword());
        }
        if (workstation.getPrivateKey() != null && !workstation.getPrivateKey().isEmpty()) {
            workstation.setEncryptedPrivateKey(encrypt(workstation.getPrivateKey()));
        } else {
            workstation.setEncryptedPrivateKey(existing.getEncryptedPrivateKey());
        }

        workstationMapper.update(workstation);
        logger.info("更新工作站点: {} ({})", workstation.getName(), workstation.getIpAddress());
        return workstation;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Workstation existing = getById(id);
        workstationMapper.deleteById(id);
        logger.info("删除工作站点: {} ({})", existing.getName(), existing.getIpAddress());
    }

    @Override
    public Map<String, Object> testConnection(Workstation workstation) {
        Map<String, Object> result = new HashMap<>();
        Session session = null;

        try {
            JSch jsch = new JSch();

            // 配置认证方式
            if ("privateKey".equals(workstation.getAuthType()) && workstation.getPrivateKey() != null) {
                byte[] privateKeyBytes = workstation.getPrivateKey().getBytes();
                jsch.addIdentity("workstation-key", privateKeyBytes, null, null);
            }

            session = jsch.getSession(
                    workstation.getSshUser(),
                    workstation.getIpAddress(),
                    workstation.getSshPort() != null ? workstation.getSshPort() : 22
            );

            if ("password".equals(workstation.getAuthType()) && workstation.getPassword() != null) {
                session.setPassword(workstation.getPassword());
            }

            // 配置 SSH
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,password");
            session.setConfig(config);
            session.setTimeout(30000); // 30秒超时

            // 连接
            session.connect();
            result.put("success", true);
            result.put("message", "连接成功");

            // 获取服务器资源信息
            Map<String, Object> resourceInfo = getServerResources(session);
            result.putAll(resourceInfo);

            logger.info("测试连接成功: {}@{}:{}", workstation.getSshUser(),
                    workstation.getIpAddress(), workstation.getSshPort());

        } catch (JSchException e) {
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
            logger.error("测试连接失败: {}@{}:{} - {}",
                    workstation.getSshUser(), workstation.getIpAddress(),
                    workstation.getSshPort(), e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> checkStatus(Long id) {
        Workstation workstation = getById(id);
        Map<String, Object> result = new HashMap<>();
        Session session = null;

        try {
            // 加载凭证
            String password = workstation.getEncryptedPassword() != null ?
                    decrypt(workstation.getEncryptedPassword()) : null;
            String privateKey = workstation.getEncryptedPrivateKey() != null ?
                    decrypt(workstation.getEncryptedPrivateKey()) : null;

            JSch jsch = new JSch();
            if ("privateKey".equals(workstation.getAuthType()) && privateKey != null) {
                jsch.addIdentity("workstation-key", privateKey.getBytes(), null, null);
            }

            session = jsch.getSession(
                    workstation.getSshUser(),
                    workstation.getIpAddress(),
                    workstation.getSshPort()
            );

            if ("password".equals(workstation.getAuthType()) && password != null) {
                session.setPassword(password);
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(10000); // 10秒超时

            session.connect();

            // 获取资源信息
            Map<String, Object> resourceInfo = getServerResources(session);
            result.put("success", true);
            result.put("status", "online");
            result.putAll(resourceInfo);

            // 更新数据库
            Integer availableDiskGb = resourceInfo.get("availableDiskGb") != null ?
                    (Integer) resourceInfo.get("availableDiskGb") : 0;
            Integer availableRamGb = resourceInfo.get("availableRamGb") != null ?
                    (Integer) resourceInfo.get("availableRamGb") : 0;

            workstationMapper.updateHeartbeat(id, "online", LocalDateTime.now(),
                    objectMapper.writeValueAsString(resourceInfo), availableDiskGb, availableRamGb);

        } catch (Exception e) {
            result.put("success", false);
            result.put("status", "offline");
            result.put("message", e.getMessage());

            // 更新状态为离线
            workstationMapper.updateStatus(id, "offline");
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    @Override
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次心跳检测
    public void heartbeatCheck() {
        logger.info("开始执行工作站点心跳检测...");
        List<Workstation> workstations = workstationMapper.findAll();

        for (Workstation workstation : workstations) {
            try {
                checkStatus(workstation.getId());
            } catch (Exception e) {
                logger.error("心跳检测失败: {} - {}", workstation.getName(), e.getMessage());
            }
        }

        logger.info("工作站点心跳检测完成, 共检测 {} 个站点", workstations.size());
    }

    @Override
    public boolean isNameExists(String name) {
        return workstationMapper.countByName(name) > 0;
    }

    @Override
    public boolean isHostnameExists(String hostname) {
        return workstationMapper.countByHostname(hostname) > 0;
    }

    @Override
    public List<Workstation> getOnlineWorkstations() {
        return workstationMapper.findOnlineWorkstations();
    }

    @Override
    public Workstation getByHostname(String hostname) {
        return workstationMapper.findByHostname(hostname)
                .orElseThrow(() -> new RuntimeException("工作站点不存在: hostname=" + hostname));
    }

    @Override
    public Map<String, Object> getCredentials(Long id) {
        Workstation workstation = getById(id);
        Map<String, Object> credentials = new HashMap<>();

        credentials.put("id", workstation.getId());
        credentials.put("ipAddress", workstation.getIpAddress());
        credentials.put("sshPort", workstation.getSshPort());
        credentials.put("sshUser", workstation.getSshUser());
        credentials.put("authType", workstation.getAuthType());
        credentials.put("hostname", workstation.getHostname());
        credentials.put("osType", workstation.getOsType());

        // 解密并返回凭证
        if ("password".equals(workstation.getAuthType()) && workstation.getEncryptedPassword() != null) {
            credentials.put("password", decrypt(workstation.getEncryptedPassword()));
        }
        if ("privateKey".equals(workstation.getAuthType()) && workstation.getEncryptedPrivateKey() != null) {
            credentials.put("privateKey", decrypt(workstation.getEncryptedPrivateKey()));
        }

        return credentials;
    }

    /**
     * 获取服务器资源信息
     */
    private Map<String, Object> getServerResources(Session session) {
        Map<String, Object> resources = new HashMap<>();

        try {
            // 获取操作系统信息
            String osInfo = executeCommand(session, "cat /etc/os-release 2>/dev/null || uname -a");
            resources.put("osInfo", osInfo);

            // 获取 CPU 信息
            String cpuInfo = executeCommand(session, "nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 1");
            try {
                resources.put("cpuCores", Integer.parseInt(cpuInfo.trim()));
            } catch (NumberFormatException e) {
                resources.put("cpuCores", 1);
            }

            // 获取内存信息 (MB)
            String memInfo = executeCommand(session,
                "free -m 2>/dev/null | grep Mem | awk '{print $2,$7}' || " +
                "sysctl -n hw.memsize 2>/dev/null | awk '{print int($1/1024/1024)}'"
            );
            String[] memParts = memInfo.trim().split("\\s+");
            if (memParts.length >= 1) {
                try {
                    int totalRamMb = Integer.parseInt(memParts[0]);
                    resources.put("ramGb", totalRamMb / 1024);
                    if (memParts.length >= 2) {
                        int availableRamMb = Integer.parseInt(memParts[1]);
                        resources.put("availableRamGb", availableRamMb / 1024);
                    }
                } catch (NumberFormatException e) {
                    resources.put("ramGb", 1);
                    resources.put("availableRamGb", 0);
                }
            }

            // 获取磁盘信息 (GB)
            String diskInfo = executeCommand(session,
                "df -BG / 2>/dev/null | tail -1 | awk '{print $2,$4}' | tr -d 'G'"
            );
            String[] diskParts = diskInfo.trim().split("\\s+");
            if (diskParts.length >= 1) {
                try {
                    resources.put("diskGb", Integer.parseInt(diskParts[0]));
                    if (diskParts.length >= 2) {
                        resources.put("availableDiskGb", Integer.parseInt(diskParts[1]));
                    }
                } catch (NumberFormatException e) {
                    resources.put("diskGb", 10);
                    resources.put("availableDiskGb", 0);
                }
            }

            // 获取主机名
            String hostname = executeCommand(session, "hostname");
            resources.put("hostname", hostname.trim());

            // 获取 GPU 信息
            Map<String, Object> gpuInfo = getGpuInfo(session);
            resources.putAll(gpuInfo);

        } catch (Exception e) {
            logger.error("获取服务器资源信息失败: {}", e.getMessage());
        }

        return resources;
    }

    /**
     * 获取 GPU 信息
     */
    private Map<String, Object> getGpuInfo(Session session) {
        Map<String, Object> gpuInfo = new HashMap<>();
        gpuInfo.put("hasGpu", false);
        gpuInfo.put("gpuCount", 0);

        try {
            // 检查是否有 nvidia-smi 命令
            String nvidiaSmi = executeCommand(session, "which nvidia-smi 2>/dev/null");
            if (nvidiaSmi.trim().isEmpty()) {
                return gpuInfo;
            }

            // 获取 GPU 数量
            String gpuCountStr = executeCommand(session, "nvidia-smi -L 2>/dev/null | wc -l");
            int gpuCount = 0;
            try {
                gpuCount = Integer.parseInt(gpuCountStr.trim());
            } catch (NumberFormatException e) {
                return gpuInfo;
            }

            if (gpuCount > 0) {
                gpuInfo.put("hasGpu", true);
                gpuInfo.put("gpuCount", gpuCount);
                gpuInfo.put("gpuType", gpuCount > 1 ? "multi_gpu" : "single_gpu");

                // 获取 GPU 型号
                String gpuModel = executeCommand(session,
                    "nvidia-smi --query-gpu=gpu_name --format=csv,noheader 2>/dev/null | head -1");
                if (!gpuModel.trim().isEmpty()) {
                    gpuInfo.put("gpuModel", gpuModel.trim());
                }

                // 获取 GPU 显存 (MB -> GB)
                String gpuMemoryStr = executeCommand(session,
                    "nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits 2>/dev/null | head -1");
                try {
                    int gpuMemoryMb = Integer.parseInt(gpuMemoryStr.trim());
                    gpuInfo.put("gpuMemory", gpuMemoryMb / 1024); // 转换为 GB
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        } catch (Exception e) {
            logger.warn("获取 GPU 信息失败: {}", e.getMessage());
        }

        return gpuInfo;
    }

    /**
     * 执行远程命令
     */
    private String executeCommand(Session session, String command) throws JSchException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            return output.toString();
        } catch (Exception e) {
            throw new JSchException("执行命令失败: " + e.getMessage());
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    // ==================== 加密解密 ====================

    private String encrypt(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    private String decrypt(String encryptedData) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
