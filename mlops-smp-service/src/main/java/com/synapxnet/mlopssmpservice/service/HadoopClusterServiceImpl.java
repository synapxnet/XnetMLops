package com.synapxnet.mlopssmpservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import com.synapxnet.mlopssmpservice.entity.HadoopCluster;
import com.synapxnet.mlopssmpservice.entity.HadoopDeployConfig;
import com.synapxnet.mlopssmpservice.mapper.HadoopClusterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Hadoop 集群服务实现
 */
@Service
public class HadoopClusterServiceImpl implements HadoopClusterService {

    private static final Logger logger = LoggerFactory.getLogger(HadoopClusterServiceImpl.class);

    @Autowired
    private HadoopClusterMapper hadoopClusterMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ClusterManagementService clusterManagementService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== CRUD 操作 ====================

    @Override
    public List<HadoopCluster> getAll() {
        return hadoopClusterMapper.findAll();
    }

    @Override
    public List<HadoopCluster> getMasters() {
        return hadoopClusterMapper.findMasters();
    }

    @Override
    public List<HadoopCluster> getNodes() {
        return hadoopClusterMapper.findNodes();
    }

    @Override
    public Optional<HadoopCluster> getById(Long id) {
        return hadoopClusterMapper.findById(id);
    }

    @Override
    public Optional<HadoopCluster> getByUid(String uid) {
        return hadoopClusterMapper.findByUid(uid);
    }

    @Override
    @Transactional
    public HadoopCluster create(HadoopCluster cluster, String userId) {
        // 生成唯一标识
        cluster.setUid(UUID.randomUUID().toString());
        cluster.setStatus("created");
        cluster.setCreatedBy(userId);

        // 设置默认值
        if (cluster.getPort() == null) {
            cluster.setPort(22);
        }
        if (cluster.getHdfsReplication() == null) {
            cluster.setHdfsReplication(3);
        }
        if (cluster.getHdfsBlockSize() == null) {
            cluster.setHdfsBlockSize(134217728L); // 128MB
        }
        if (cluster.getYarnMemory() == null) {
            cluster.setYarnMemory(8192);
        }
        if (cluster.getYarnCpu() == null) {
            cluster.setYarnCpu(4);
        }
        if (cluster.getDeployMode() == null) {
            cluster.setDeployMode("standard");
        }

        hadoopClusterMapper.insert(cluster);

        // 如果是Node节点，自动触发Hosts同步到集群所有节点
        if ("node".equals(cluster.getNodeType()) && cluster.getMasterId() != null) {
            final Long masterId = cluster.getMasterId();
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("Node节点创建成功，自动触发Hosts同步: masterId={}, nodeId={}", masterId, cluster.getId());
                    clusterManagementService.syncHostsToCluster("hadoop", masterId);
                } catch (Exception e) {
                    logger.error("自动Hosts同步失败: masterId={}, error={}", masterId, e.getMessage());
                }
            });
        }

        return cluster;
    }

    @Override
    @Transactional
    public HadoopCluster update(Long id, HadoopCluster cluster, String userId) {
        Optional<HadoopCluster> existing = hadoopClusterMapper.findById(id);
        if (existing.isEmpty()) {
            throw new RuntimeException("节点不存在: " + id);
        }

        cluster.setId(id);
        hadoopClusterMapper.update(cluster);
        return cluster;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        Optional<HadoopCluster> existing = hadoopClusterMapper.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        // 检查是否有关联的 Node 节点
        List<HadoopCluster> nodes = hadoopClusterMapper.findByMasterId(id);
        if (!nodes.isEmpty()) {
            throw new RuntimeException("存在关联的 Node 节点，请先删除 Node 节点");
        }

        hadoopClusterMapper.deleteById(id);
        return true;
    }

    // ==================== 部署操作 ====================

    @Override
    public Map<String, Object> testConnection(HadoopCluster cluster) {
        Map<String, Object> result = new HashMap<>();
        Session session = null;

        try {
            JSch jsch = new JSch();

            // 如果提供了私钥
            if (cluster.getSshPrivateKey() != null && !cluster.getSshPrivateKey().isEmpty()) {
                jsch.addIdentity("key", cluster.getSshPrivateKey().getBytes(), null, null);
            }

            session = jsch.getSession(cluster.getSshUser(), cluster.getHost(), cluster.getPort());

            // 如果没有私钥，使用密码
            if (cluster.getSshPrivateKey() == null || cluster.getSshPrivateKey().isEmpty()) {
                session.setPassword(cluster.getSshPassword());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(10000);
            session.connect();

            // 获取系统信息 - 参照 Jenkins 实现
            String osInfo = executeCommand(session, "uname -a 2>/dev/null || ver 2>nul || echo 'Unknown'");
            String hostname = executeCommand(session, "hostname");

            // 检查磁盘空间
            String diskInfo = executeCommand(session, "df -h / 2>/dev/null | tail -1 | awk '{print $4}'");

            // 检查内存
            String memInfo = executeCommand(session, "free -m 2>/dev/null | grep Mem | awk '{print $2}'");

            // 检测操作系统类型
            String osRelease = executeCommand(session, "cat /etc/os-release 2>/dev/null | head -5");
            String detectedOs = detectOsType(osInfo);

            result.put("success", true);
            result.put("message", "连接成功");
            result.put("osInfo", osInfo.trim() + "\n" + osRelease.trim());
            result.put("hostname", hostname.trim());
            result.put("availableDisk", diskInfo.trim());
            result.put("totalMemoryMb", memInfo.trim());
            result.put("detectedOsType", detectedOs);

        } catch (Exception e) {
            logger.error("SSH 连接测试失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    private String executeCommand(Session session, String command) {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            channel.connect(5000);

            int timeout = 0;
            while (!channel.isClosed() && timeout < 50) {
                Thread.sleep(100);
                timeout++;
            }

            String output = outputStream.toString(StandardCharsets.UTF_8);
            channel.disconnect();
            return output;
        } catch (Exception e) {
            logger.warn("执行命令失败: {} - {}", command, e.getMessage());
            return "";
        }
    }

    private String detectOsType(String osInfo) {
        String info = osInfo.toLowerCase();
        if (info.contains("darwin") || info.contains("macos")) {
            return "macos";
        } else if (info.contains("windows")) {
            return "windows";
        }
        return "linux";
    }


    @Override
    public Map<String, Object> deploy(Long id, HadoopDeployConfig config) {
        Map<String, Object> result = new HashMap<>();

        Optional<HadoopCluster> clusterOpt = hadoopClusterMapper.findById(id);
        if (clusterOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "节点不存在");
            return result;
        }

        HadoopCluster cluster = clusterOpt.get();

        // 更新状态为部署中
        hadoopClusterMapper.updateStatus(id, "deploying", "开始部署...\n");

        result.put("success", true);
        result.put("message", "部署任务已启动");
        result.put("clusterId", id);

        // 通过 ApplicationContext 获取代理对象，确保 @Async 生效
        HadoopClusterServiceImpl proxy = applicationContext.getBean(HadoopClusterServiceImpl.class);
        proxy.deployAsync(id, config);

        return result;
    }

    /**
     * 异步执行部署任务
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deployAsync(Long id, HadoopDeployConfig config) {
        Optional<HadoopCluster> clusterOpt = hadoopClusterMapper.findById(id);
        if (clusterOpt.isEmpty()) {
            logger.error("部署失败，节点不存在: {}", id);
            return;
        }

        HadoopCluster cluster = clusterOpt.get();

        try {
            // 生成部署脚本
            String script = generateDeployScript(cluster, config);

            // 执行部署
            StringBuilder log = new StringBuilder();
            log.append("=== 开始部署 Hadoop ").append(cluster.getNodeType()).append(" ===\n");
            log.append("时间: ").append(new Date()).append("\n");
            log.append("目标主机: ").append(cluster.getHost()).append("\n");
            log.append("Hadoop版本: ").append(config.getHadoopVersion()).append("\n\n");

            boolean success = executeRemoteScript(cluster, script, log);

            if (success) {
                hadoopClusterMapper.updateStatus(id, "deployed", log.toString());
                logger.info("Hadoop 部署成功: {}", cluster.getHost());
            } else {
                hadoopClusterMapper.updateStatus(id, "failed", log.toString());
                logger.error("Hadoop 部署失败: {}", cluster.getHost());
            }

        } catch (Exception e) {
            logger.error("部署失败: {}", e.getMessage(), e);
            hadoopClusterMapper.updateStatus(id, "failed", "部署异常: " + e.getMessage());
        }
    }

    @Override
    public String previewScript(String osType, String nodeType, HadoopDeployConfig config) {
        HadoopCluster cluster = new HadoopCluster();
        cluster.setOsType(osType);
        cluster.setNodeType(nodeType);
        cluster.setHadoopVersion(config.getHadoopVersion());

        try {
            return generateDeployScript(cluster, config);
        } catch (Exception e) {
            return "生成脚本失败: " + e.getMessage();
        }
    }

    // ==================== 状态操作 ====================

    @Override
    public Map<String, Object> checkStatus(Long id) {
        Map<String, Object> result = new HashMap<>();

        Optional<HadoopCluster> clusterOpt = hadoopClusterMapper.findById(id);
        if (clusterOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "节点不存在");
            return result;
        }

        HadoopCluster cluster = clusterOpt.get();

        try {
            String command = "jps 2>/dev/null | grep -E 'NameNode|DataNode|ResourceManager|NodeManager' || echo 'No Hadoop processes'";
            String output = executeRemoteCommand(cluster, command);

            result.put("success", true);
            result.put("processes", output);

            // 判断状态
            if (output.contains("NameNode") || output.contains("DataNode")) {
                result.put("status", "running");
            } else {
                result.put("status", "stopped");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查状态失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> startServices(Long id) {
        return executeHadoopCommand(id, "start");
    }

    @Override
    public Map<String, Object> stopServices(Long id) {
        return executeHadoopCommand(id, "stop");
    }

    @Override
    public Map<String, Object> restartServices(Long id) {
        Map<String, Object> stopResult = stopServices(id);
        if (!(Boolean) stopResult.getOrDefault("success", false)) {
            return stopResult;
        }
        return startServices(id);
    }

    // ==================== 集群健康 ====================

    @Override
    public Map<String, Object> getClusterHealth(Long masterId) {
        Map<String, Object> health = new HashMap<>();

        health.put("hdfs", getHdfsStatus(masterId));
        health.put("yarn", getYarnStatus(masterId));

        // 获取 Node 节点数量
        List<HadoopCluster> nodes = hadoopClusterMapper.findByMasterId(masterId);
        health.put("nodeCount", nodes.size());
        health.put("runningNodes", nodes.stream().filter(n -> "running".equals(n.getStatus())).count());

        return health;
    }

    @Override
    public Map<String, Object> getHdfsStatus(Long masterId) {
        Map<String, Object> result = new HashMap<>();

        Optional<HadoopCluster> masterOpt = hadoopClusterMapper.findById(masterId);
        if (masterOpt.isEmpty()) {
            result.put("success", false);
            return result;
        }

        try {
            String command = "hdfs dfsadmin -report 2>/dev/null | head -20";
            String output = executeRemoteCommand(masterOpt.get(), command);
            result.put("success", true);
            result.put("report", output);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getYarnStatus(Long masterId) {
        Map<String, Object> result = new HashMap<>();

        Optional<HadoopCluster> masterOpt = hadoopClusterMapper.findById(masterId);
        if (masterOpt.isEmpty()) {
            result.put("success", false);
            return result;
        }

        try {
            String command = "yarn node -list 2>/dev/null";
            String output = executeRemoteCommand(masterOpt.get(), command);
            result.put("success", true);
            result.put("nodes", output);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ==================== 私有方法 ====================

    private String generateDeployScript(HadoopCluster cluster, HadoopDeployConfig config) throws IOException {
        // 根据操作系统类型和节点类型选择脚本
        String osType = config.getOsType() != null ? config.getOsType() : "linux";
        String nodeType = cluster.getNodeType() != null ? cluster.getNodeType() : "master";

        String templatePath;
        if ("windows".equalsIgnoreCase(osType)) {
            // Windows 脚本 (PowerShell)
            templatePath = "master".equals(nodeType)
                    ? "scripts/hadoop-master-windows.ps1"
                    : "scripts/hadoop-node-windows.ps1";
        } else if ("macos".equalsIgnoreCase(osType)) {
            // macOS 脚本 (与 Linux 类似，使用相同脚本)
            templatePath = "master".equals(nodeType)
                    ? "scripts/hadoop-master-linux.sh"
                    : "scripts/hadoop-node-linux.sh";
        } else {
            // Linux 脚本 (默认)
            templatePath = "master".equals(nodeType)
                    ? "scripts/hadoop-master-linux.sh"
                    : "scripts/hadoop-node-linux.sh";
        }

        ClassPathResource resource = new ClassPathResource(templatePath);
        String script;

        try {
            script = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 模板不存在时使用内置模板
            script = getDefaultDeployScript(cluster, config);
        }

        // 替换基础变量 - 同时支持 ${VAR} 和 ${VAR:-default} 两种格式
        String hadoopVersion = config.getHadoopVersion() != null ? config.getHadoopVersion() : "3.3.6";
        String deployMode = config.getDeployMode() != null ? config.getDeployMode() : "standard";
        String javaVersion = config.getJavaVersion() != null ? config.getJavaVersion() : "8";
        int hdfsReplication = config.getHdfsReplication() != null ? config.getHdfsReplication() : 3;
        long hdfsBlockSize = config.getHdfsBlockSizeMb() != null ? config.getHdfsBlockSizeMb() * 1024 * 1024 : 134217728;
        int yarnMemory = config.getYarnMemory() != null ? config.getYarnMemory() : 8192;
        int yarnCpu = config.getYarnCpu() != null ? config.getYarnCpu() : 4;

        // 替换带默认值的变量格式 ${VAR:-default}
        script = script.replaceAll("\\$\\{HADOOP_VERSION:-[^}]*\\}", hadoopVersion);
        script = script.replaceAll("\\$\\{OS_TYPE:-[^}]*\\}", osType);
        script = script.replaceAll("\\$\\{DEPLOY_MODE:-[^}]*\\}", deployMode);
        script = script.replaceAll("\\$\\{JAVA_VERSION:-[^}]*\\}", javaVersion);
        script = script.replaceAll("\\$\\{HDFS_REPLICATION:-[^}]*\\}", String.valueOf(hdfsReplication));
        script = script.replaceAll("\\$\\{HDFS_BLOCK_SIZE:-[^}]*\\}", String.valueOf(hdfsBlockSize));
        script = script.replaceAll("\\$\\{YARN_MEMORY:-[^}]*\\}", String.valueOf(yarnMemory));
        script = script.replaceAll("\\$\\{YARN_CPU:-[^}]*\\}", String.valueOf(yarnCpu));

        // 确定 MASTER_HOST：对于 Node 类型的节点，需要获取关联 Master 的 IP 地址
        String masterHost = cluster.getHost(); // 默认使用当前节点地址（适用于 Master 节点）
        if ("node".equalsIgnoreCase(nodeType) && cluster.getMasterId() != null) {
            // Node 节点：查询关联 Master 的 IP 地址
            Optional<HadoopCluster> masterOpt = hadoopClusterMapper.findById(cluster.getMasterId());
            if (masterOpt.isPresent()) {
                masterHost = masterOpt.get().getHost();
                logger.info("Node 部署使用 Master 地址: {}", masterHost);
            } else {
                logger.warn("未找到关联的 Master 节点 (ID: {})，使用当前节点地址", cluster.getMasterId());
            }
        }

        script = script.replaceAll("\\$\\{MASTER_HOST:-[^}]*\\}", masterHost);
        script = script.replaceAll("\\$\\{HA_MASTER_HOST:-[^}]*\\}", config.getHaMasterHost() != null ? config.getHaMasterHost() : "");
        script = script.replaceAll("\\$\\{ZK_CLUSTER:-[^}]*\\}", config.getZkCluster() != null ? config.getZkCluster() : "");

        // 替换简单变量格式 ${VAR}
        script = script.replace("${HADOOP_VERSION}", hadoopVersion);
        script = script.replace("${OS_TYPE}", osType);
        script = script.replace("${DEPLOY_MODE}", deployMode);
        script = script.replace("${JAVA_VERSION}", javaVersion);
        script = script.replace("${HDFS_REPLICATION}", String.valueOf(hdfsReplication));
        script = script.replace("${HDFS_BLOCK_SIZE}", String.valueOf(hdfsBlockSize));
        script = script.replace("${YARN_MEMORY}", String.valueOf(yarnMemory));
        script = script.replace("${YARN_CPU}", String.valueOf(yarnCpu));
        script = script.replace("${MASTER_HOST}", masterHost);

        // 数据目录
        String dataDirs = config.getHdfsDataDirs() != null && !config.getHdfsDataDirs().isEmpty()
                ? String.join(",", config.getHdfsDataDirs())
                : "/data/hadoop/hdfs";
        script = script.replaceAll("\\$\\{HDFS_DATA_DIRS:-[^}]*\\}", dataDirs);
        script = script.replace("${HDFS_DATA_DIRS}", dataDirs);

        // 端口配置
        script = script.replaceAll("\\$\\{NAMENODE_PORT:-[^}]*\\}", String.valueOf(config.getNameNodePort()));
        script = script.replaceAll("\\$\\{NAMENODE_HTTP_PORT:-[^}]*\\}", String.valueOf(config.getNameNodeHttpPort()));
        script = script.replaceAll("\\$\\{DATANODE_PORT:-[^}]*\\}", String.valueOf(config.getDataNodePort()));
        script = script.replaceAll("\\$\\{SECONDARY_NAMENODE_HTTP_PORT:-[^}]*\\}", String.valueOf(config.getSecondaryNameNodeHttpPort()));
        script = script.replaceAll("\\$\\{RESOURCEMANAGER_PORT:-[^}]*\\}", String.valueOf(config.getResourceManagerPort()));
        script = script.replaceAll("\\$\\{RESOURCEMANAGER_WEB_PORT:-[^}]*\\}", String.valueOf(config.getResourceManagerWebPort()));
        script = script.replaceAll("\\$\\{NODEMANAGER_PORT:-[^}]*\\}", String.valueOf(config.getNodeManagerPort()));
        script = script.replaceAll("\\$\\{JOBHISTORY_PORT:-[^}]*\\}", String.valueOf(config.getJobHistoryPort()));
        script = script.replaceAll("\\$\\{JOBHISTORY_WEB_PORT:-[^}]*\\}", String.valueOf(config.getJobHistoryWebPort()));

        script = script.replace("${NAMENODE_PORT}", String.valueOf(config.getNameNodePort()));
        script = script.replace("${NAMENODE_HTTP_PORT}", String.valueOf(config.getNameNodeHttpPort()));
        script = script.replace("${DATANODE_PORT}", String.valueOf(config.getDataNodePort()));
        script = script.replace("${SECONDARY_NAMENODE_HTTP_PORT}", String.valueOf(config.getSecondaryNameNodeHttpPort()));
        script = script.replace("${RESOURCEMANAGER_PORT}", String.valueOf(config.getResourceManagerPort()));
        script = script.replace("${RESOURCEMANAGER_WEB_PORT}", String.valueOf(config.getResourceManagerWebPort()));
        script = script.replace("${NODEMANAGER_PORT}", String.valueOf(config.getNodeManagerPort()));
        script = script.replace("${JOBHISTORY_PORT}", String.valueOf(config.getJobHistoryPort()));
        script = script.replace("${JOBHISTORY_WEB_PORT}", String.valueOf(config.getJobHistoryWebPort()));

        // HA 配置
        if ("ha".equals(config.getDeployMode())) {
            script = script.replace("${HA_MASTER_HOST}", config.getHaMasterHost() != null ? config.getHaMasterHost() : "");
            script = script.replace("${ZK_CLUSTER}", config.getZkCluster() != null ? config.getZkCluster() : "");
        }

        // Master 主机地址
        script = script.replace("${MASTER_HOST}", cluster.getHost());

        // 跨云部署配置 - 生成hosts配置脚本
        String hostsConfigScript = generateHostsConfigScript(config);
        script = script.replace("${HOSTS_CONFIG_SCRIPT}", hostsConfigScript);

        // 跨云模式下使用hostname替代IP
        boolean crossCloudMode = Boolean.TRUE.equals(config.getCrossCloudMode());
        script = script.replace("${CROSS_CLOUD_MODE}", String.valueOf(crossCloudMode));

        String nodeHostname = config.getNodeHostname();
        if (nodeHostname == null || nodeHostname.isEmpty()) {
            nodeHostname = cluster.getHost(); // 默认使用IP
        }
        script = script.replace("${NODE_HOSTNAME}", nodeHostname);

        // 跨云模式下，Master地址使用hostname
        String masterHostForConfig = masterHost;
        if (crossCloudMode && "node".equalsIgnoreCase(nodeType) && cluster.getMasterId() != null) {
            Optional<HadoopCluster> masterOpt = hadoopClusterMapper.findById(cluster.getMasterId());
            if (masterOpt.isPresent()) {
                // 尝试从clusterHosts中查找master的hostname
                if (config.getClusterHosts() != null) {
                    for (String hostEntry : config.getClusterHosts()) {
                        String[] parts = hostEntry.trim().split("\\s+");
                        if (parts.length >= 2 && parts[0].equals(masterOpt.get().getHost())) {
                            masterHostForConfig = parts[1];
                            break;
                        }
                    }
                }
            }
        }
        script = script.replace("${MASTER_HOST_FOR_CONFIG}", masterHostForConfig);

        return script;
    }



    private String getDefaultDeployScript(HadoopCluster cluster, HadoopDeployConfig config) {
        // 返回基础部署脚本模板
        return "#!/bin/bash\n" +
                "echo 'Hadoop 部署脚本'\n" +
                "echo 'Hadoop 版本: ${HADOOP_VERSION}'\n" +
                "echo '部署模式: ${DEPLOY_MODE}'\n" +
                "echo '请确保已创建部署脚本模板'\n";
    }

    /**
     * 生成/etc/hosts配置脚本
     * 用于跨云/跨地域部署时配置集群节点hostname解析
     */
    private String generateHostsConfigScript(HadoopDeployConfig config) {
        if (!Boolean.TRUE.equals(config.getCrossCloudMode()) ||
            config.getClusterHosts() == null ||
            config.getClusterHosts().isEmpty()) {
            return "# 跨云模式未启用或无hosts配置";
        }

        StringBuilder script = new StringBuilder();
        script.append("echo '>>> 配置集群节点 /etc/hosts...'\n");
        script.append("# 备份原有hosts文件\n");
        script.append("cp /etc/hosts /etc/hosts.backup.$(date +%Y%m%d%H%M%S)\n\n");
        script.append("# 添加集群节点hosts配置\n");
        script.append("cat >> /etc/hosts << 'HADOOP_HOSTS_EOF'\n");
        script.append("\n# === Hadoop Cluster Hosts (Added by XnetMLops) ===\n");

        for (String hostEntry : config.getClusterHosts()) {
            String trimmed = hostEntry.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                script.append(trimmed).append("\n");
            }
        }

        script.append("# === End of Hadoop Cluster Hosts ===\n");
        script.append("HADOOP_HOSTS_EOF\n\n");
        script.append("echo '>>> /etc/hosts 配置完成'\n");
        script.append("echo '当前 hosts 配置:'\n");
        script.append("cat /etc/hosts | grep -v '^#' | grep -v '^$'\n");

        return script.toString();
    }

    private boolean executeRemoteScript(HadoopCluster cluster, String script, StringBuilder log) {
        Session session = null;
        try {
            JSch jsch = new JSch();

            if (cluster.getSshPrivateKey() != null && !cluster.getSshPrivateKey().isEmpty()) {
                jsch.addIdentity("key", cluster.getSshPrivateKey().getBytes(), null, null);
            }

            session = jsch.getSession(cluster.getSshUser(), cluster.getHost(), cluster.getPort());

            if (cluster.getSshPrivateKey() == null || cluster.getSshPrivateKey().isEmpty()) {
                session.setPassword(cluster.getSshPassword());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(30000);
            session.connect();

            // 上传脚本
            String scriptPath = "/tmp/hadoop_deploy_" + System.currentTimeMillis() + ".sh";
            uploadFile(session, script.getBytes(StandardCharsets.UTF_8), scriptPath);

            // 执行脚本
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("chmod +x " + scriptPath + " && " + scriptPath);

            InputStream inputStream = channel.getInputStream();
            InputStream errStream = channel.getErrStream();
            channel.connect();

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append("\n");
                logger.info("[DEPLOY] {}", line);
            }
            while ((line = errReader.readLine()) != null) {
                log.append("[ERROR] ").append(line).append("\n");
                logger.warn("[DEPLOY ERROR] {}", line);
            }

            // 等待完成
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            int exitStatus = channel.getExitStatus();
            channel.disconnect();

            log.append("\n=== 部署完成，退出码: ").append(exitStatus).append(" ===\n");
            return exitStatus == 0;

        } catch (Exception e) {
            log.append("\n=== 部署异常: ").append(e.getMessage()).append(" ===\n");
            logger.error("执行远程脚本失败", e);
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void uploadFile(Session session, byte[] content, String remotePath) throws JSchException, IOException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            sftp.put(bais, remotePath);
        } catch (Exception e) {
            throw new IOException("上传文件失败: " + e.getMessage(), e);
        } finally {
            sftp.disconnect();
        }
    }

    private String executeRemoteCommand(HadoopCluster cluster, String command) throws Exception {
        Session session = null;
        try {
            JSch jsch = new JSch();

            if (cluster.getSshPrivateKey() != null && !cluster.getSshPrivateKey().isEmpty()) {
                jsch.addIdentity("key", cluster.getSshPrivateKey().getBytes(), null, null);
            }

            session = jsch.getSession(cluster.getSshUser(), cluster.getHost(), cluster.getPort());

            if (cluster.getSshPrivateKey() == null || cluster.getSshPrivateKey().isEmpty()) {
                session.setPassword(cluster.getSshPassword());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(10000);
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            channel.connect(5000);

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            channel.disconnect();
            return outputStream.toString(StandardCharsets.UTF_8);

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private Map<String, Object> executeHadoopCommand(Long id, String action) {
        Map<String, Object> result = new HashMap<>();

        Optional<HadoopCluster> clusterOpt = hadoopClusterMapper.findById(id);
        if (clusterOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "节点不存在");
            return result;
        }

        HadoopCluster cluster = clusterOpt.get();

        try {
            String command;
            if ("master".equals(cluster.getNodeType())) {
                if ("start".equals(action)) {
                    command = "start-dfs.sh && start-yarn.sh";
                } else {
                    command = "stop-yarn.sh && stop-dfs.sh";
                }
            } else {
                if ("start".equals(action)) {
                    command = "hdfs --daemon start datanode && yarn --daemon start nodemanager";
                } else {
                    command = "yarn --daemon stop nodemanager && hdfs --daemon stop datanode";
                }
            }

            String output = executeRemoteCommand(cluster, command);

            // 更新状态
            String newStatus = "start".equals(action) ? "running" : "stopped";
            hadoopClusterMapper.updateStatus(id, newStatus, null);

            result.put("success", true);
            result.put("message", action + " 操作成功");
            result.put("output", output);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", action + " 操作失败: " + e.getMessage());
        }

        return result;
    }
}
