package com.synapxnet.mlopssmpservice.service;

import com.jcraft.jsch.*;
import com.synapxnet.mlopssmpservice.entity.HadoopCluster;
import com.synapxnet.mlopssmpservice.entity.JenkinsMaster;
import com.synapxnet.mlopssmpservice.entity.JenkinsNode;
import com.synapxnet.mlopssmpservice.mapper.HadoopClusterMapper;
import com.synapxnet.mlopssmpservice.mapper.JenkinsMasterMapper;
import com.synapxnet.mlopssmpservice.mapper.JenkinsNodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一集群管理服务实现
 * 提供跨集群类型的统一管理功能，包含 Hosts 自动同步
 */
@Service
public class ClusterManagementServiceImpl implements ClusterManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManagementServiceImpl.class);

    @Autowired
    private HadoopClusterMapper hadoopClusterMapper;

    @Autowired
    private JenkinsMasterMapper jenkinsMasterMapper;

    @Autowired
    private JenkinsNodeMapper jenkinsNodeMapper;

    // ==================== 拓扑查询 ====================

    @Override
    public Map<String, Object> getClusterTopology(String clusterType) {
        switch (clusterType.toLowerCase()) {
            case "hadoop":
                return getHadoopTopology();
            case "jenkins":
                return getJenkinsTopology();
            case "redis":
            case "mysql":
            case "spark":
                return getEmptyTopology(clusterType, "集群类型 " + clusterType + " 即将支持");
            default:
                throw new IllegalArgumentException("不支持的集群类型: " + clusterType);
        }
    }

    private Map<String, Object> getHadoopTopology() {
        List<HadoopCluster> masters = hadoopClusterMapper.findMasters();
        List<HadoopCluster> nodes = hadoopClusterMapper.findNodes();

        // 实时刷新所有已部署节点的状态
        for (HadoopCluster master : masters) {
            if ("deployed".equals(master.getStatus()) || "running".equals(master.getStatus()) || "stopped".equals(master.getStatus())) {
                String realStatus = checkHadoopNodeRealTimeStatus(master);
                if (!realStatus.equals(master.getStatus())) {
                    logger.info("拓扑查询 - Master {} 状态更新: {} -> {}", master.getHost(), master.getStatus(), realStatus);
                    hadoopClusterMapper.updateStatusOnly(master.getId(), realStatus);
                    master.setStatus(realStatus);  // 更新内存中的状态
                }
            }
        }

        for (HadoopCluster node : nodes) {
            if ("deployed".equals(node.getStatus()) || "running".equals(node.getStatus()) || "stopped".equals(node.getStatus())) {
                String realStatus = checkHadoopNodeRealTimeStatus(node);
                if (!realStatus.equals(node.getStatus())) {
                    logger.info("拓扑查询 - Node {} 状态更新: {} -> {}", node.getHost(), node.getStatus(), realStatus);
                    hadoopClusterMapper.updateStatusOnly(node.getId(), realStatus);
                    node.setStatus(realStatus);  // 更新内存中的状态
                }
            }
        }

        List<Map<String, Object>> masterNodes = masters.stream()
                .map(this::convertHadoopToUnifiedNode)
                .collect(Collectors.toList());

        List<Map<String, Object>> workerNodes = nodes.stream()
                .map(this::convertHadoopToUnifiedNode)
                .collect(Collectors.toList());

        Map<String, Object> stats = calculateStats(masters, nodes);

        Map<String, Object> result = new HashMap<>();
        result.put("clusterType", "hadoop");
        result.put("masters", masterNodes);
        result.put("workers", workerNodes);
        result.put("stats", stats);

        return result;
    }

    private Map<String, Object> getJenkinsTopology() {
        List<JenkinsMaster> masters = jenkinsMasterMapper.findAll();
        List<JenkinsNode> nodes = jenkinsNodeMapper.findAll();

        List<Map<String, Object>> masterNodes = masters.stream()
                .map(this::convertJenkinsMasterToUnifiedNode)
                .collect(Collectors.toList());

        List<Map<String, Object>> workerNodes = nodes.stream()
                .map(this::convertJenkinsNodeToUnifiedNode)
                .collect(Collectors.toList());

        int total = masters.size() + nodes.size();
        long running = masters.stream().filter(m -> isRunningStatus(m.getStatus())).count()
                + nodes.stream().filter(n -> isRunningStatus(n.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNodes", total);
        stats.put("runningNodes", running);
        stats.put("stoppedNodes", total - running);
        stats.put("failedNodes", 0);

        Map<String, Object> result = new HashMap<>();
        result.put("clusterType", "jenkins");
        result.put("masters", masterNodes);
        result.put("workers", workerNodes);
        result.put("stats", stats);

        return result;
    }

    private Map<String, Object> getEmptyTopology(String clusterType, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("clusterType", clusterType);
        result.put("masters", List.of());
        result.put("workers", List.of());
        result.put("stats", Map.of("totalNodes", 0, "runningNodes", 0, "stoppedNodes", 0, "failedNodes", 0));
        result.put("message", message);
        return result;
    }

    private Map<String, Object> calculateStats(List<HadoopCluster> masters, List<HadoopCluster> nodes) {
        int total = masters.size() + nodes.size();
        long running = masters.stream().filter(m -> isRunningStatus(m.getStatus())).count()
                + nodes.stream().filter(n -> isRunningStatus(n.getStatus())).count();
        long failed = masters.stream().filter(m -> "failed".equals(m.getStatus())).count()
                + nodes.stream().filter(n -> "failed".equals(n.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNodes", total);
        stats.put("runningNodes", running);
        stats.put("stoppedNodes", total - running - failed);
        stats.put("failedNodes", failed);
        return stats;
    }

    private boolean isRunningStatus(String status) {
        return "running".equals(status) || "deployed".equals(status);
    }

    // ==================== 节点转换 ====================

    private Map<String, Object> convertHadoopToUnifiedNode(HadoopCluster cluster) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", cluster.getId());
        node.put("uid", cluster.getUid());
        node.put("name", cluster.getName());
        node.put("host", cluster.getHost());
        node.put("port", cluster.getPort());
        node.put("status", cluster.getStatus());
        node.put("role", "master".equals(cluster.getNodeType()) ? "master" : "worker");
        node.put("clusterType", "hadoop");
        node.put("masterId", cluster.getMasterId());
        node.put("description", cluster.getDescription());
        node.put("createdAt", cluster.getCreatedAt());

        Map<String, Object> extra = new HashMap<>();
        extra.put("hadoopVersion", cluster.getHadoopVersion());
        extra.put("deployMode", cluster.getDeployMode());
        extra.put("components", cluster.getComponents());
        extra.put("osType", cluster.getOsType());
        node.put("extra", extra);

        return node;
    }

    private Map<String, Object> convertJenkinsMasterToUnifiedNode(JenkinsMaster master) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", master.getId());
        node.put("uid", master.getUid());
        node.put("name", master.getName());
        node.put("host", master.getHost());
        node.put("port", master.getPort());
        node.put("status", master.getStatus());
        node.put("role", "master");
        node.put("clusterType", "jenkins");
        node.put("description", master.getDescription());
        node.put("createdAt", master.getCreated_at());

        Map<String, Object> extra = new HashMap<>();
        extra.put("jenkinsVersion", master.getJenkins_version());
        extra.put("jenkinsPort", master.getJenkins_port());
        extra.put("osType", master.getOs_type());
        node.put("extra", extra);

        return node;
    }

    private Map<String, Object> convertJenkinsNodeToUnifiedNode(JenkinsNode jnode) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", jnode.getId());
        node.put("uid", jnode.getUid());
        node.put("name", jnode.getName());
        node.put("host", jnode.getHost());
        node.put("port", jnode.getPort());
        node.put("status", jnode.getStatus());
        node.put("role", "worker");
        node.put("clusterType", "jenkins");
        node.put("masterHost", jnode.getJenkins_url());
        node.put("description", jnode.getDescription());
        node.put("createdAt", jnode.getCreated_at());

        Map<String, Object> extra = new HashMap<>();
        extra.put("resourceType", jnode.getResource_type());
        extra.put("labels", jnode.getLabels());
        extra.put("osType", jnode.getOs_type());
        node.put("extra", extra);

        return node;
    }

    // ==================== 集群概览 ====================

    @Override
    public Map<String, Object> getAllClustersOverview() {
        Map<String, Object> overview = new HashMap<>();

        overview.put("hadoop", getClusterTopology("hadoop"));
        overview.put("jenkins", getClusterTopology("jenkins"));
        overview.put("redis", getEmptyTopology("redis", "即将支持"));
        overview.put("mysql", getEmptyTopology("mysql", "即将支持"));
        overview.put("spark", getEmptyTopology("spark", "即将支持"));

        return overview;
    }

    // ==================== Hosts 同步 ====================

    @Override
    public Map<String, Object> syncHostsToCluster(String clusterType, Long masterId) {
        switch (clusterType.toLowerCase()) {
            case "hadoop":
                return syncHadoopHosts(masterId);
            case "jenkins":
                return Map.of("success", true, "message", "Jenkins集群无需hosts同步", "syncedNodes", 0);
            default:
                return Map.of("success", false, "message", "不支持的集群类型: " + clusterType);
        }
    }

    /**
     * 同步Hadoop集群的hosts配置到所有节点
     * 此方法会在添加Node节点后自动触发
     */
    public Map<String, Object> syncHadoopHosts(Long masterId) {
        Map<String, Object> result = new HashMap<>();
        List<String> failedNodes = new ArrayList<>();
        int syncedCount = 0;

        try {
            // 获取Master节点
            Optional<HadoopCluster> masterOpt = hadoopClusterMapper.findById(masterId);
            if (masterOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "Master节点不存在: " + masterId);
                return result;
            }
            HadoopCluster master = masterOpt.get();

            // 获取所有关联的Node节点
            List<HadoopCluster> nodes = hadoopClusterMapper.findByMasterId(masterId);

            // 生成hosts配置
            StringBuilder hostsContent = new StringBuilder();
            hostsContent.append("\n# === Hadoop Cluster Hosts (Auto-synced by XnetMLops) ===\n");
            hostsContent.append(master.getHost()).append(" hadoop-master\n");

            for (int i = 0; i < nodes.size(); i++) {
                HadoopCluster node = nodes.get(i);
                String hostname = "hadoop-node" + (i + 1);
                hostsContent.append(node.getHost()).append(" ").append(hostname).append("\n");
            }
            hostsContent.append("# === End of Hadoop Cluster Hosts ===\n");

            String hostsScript = generateHostsUpdateScript(hostsContent.toString());

            logger.info("开始同步Hosts配置到集群, masterId={}, 节点数={}", masterId, nodes.size() + 1);

            // 同步到Master
            try {
                executeHostsSync(master, hostsScript);
                syncedCount++;
                logger.info("Hosts同步成功: Master {} ({})", master.getName(), master.getHost());
            } catch (Exception e) {
                failedNodes.add(master.getHost() + " (Master)");
                logger.error("Hosts同步失败: Master {} - {}", master.getHost(), e.getMessage());
            }

            // 同步到所有Node
            for (HadoopCluster node : nodes) {
                try {
                    executeHostsSync(node, hostsScript);
                    syncedCount++;
                    logger.info("Hosts同步成功: Node {} ({})", node.getName(), node.getHost());
                } catch (Exception e) {
                    failedNodes.add(node.getHost() + " (" + node.getName() + ")");
                    logger.error("Hosts同步失败: Node {} - {}", node.getHost(), e.getMessage());
                }
            }

            boolean allSuccess = failedNodes.isEmpty();
            result.put("success", allSuccess);
            result.put("message", allSuccess ? "Hosts同步完成" : "部分节点同步失败");
            result.put("syncedNodes", syncedCount);
            result.put("totalNodes", nodes.size() + 1);
            result.put("failedNodes", failedNodes);

            logger.info("Hosts同步完成: 成功={}, 失败={}", syncedCount, failedNodes.size());

        } catch (Exception e) {
            logger.error("Hosts同步异常", e);
            result.put("success", false);
            result.put("message", "同步异常: " + e.getMessage());
            result.put("syncedNodes", syncedCount);
            result.put("failedNodes", failedNodes);
        }

        return result;
    }

    private String generateHostsUpdateScript(String hostsContent) {
        return "#!/bin/bash\n" +
                "set -e\n" +
                "echo '>>> 开始更新 /etc/hosts 配置...'\n" +
                "# 备份原hosts\n" +
                "cp /etc/hosts /etc/hosts.backup.$(date +%Y%m%d%H%M%S)\n" +
                "# 移除旧的Hadoop hosts配置\n" +
                "sed -i '/# === Hadoop Cluster Hosts/,/# === End of Hadoop Cluster Hosts ===/d' /etc/hosts 2>/dev/null || true\n" +
                "# 添加新配置\n" +
                "cat >> /etc/hosts << 'HOSTS_EOF'\n" +
                hostsContent +
                "HOSTS_EOF\n" +
                "echo '>>> /etc/hosts 配置更新完成'\n" +
                "echo '当前 Hadoop 相关 hosts 配置:'\n" +
                "grep -E 'hadoop-master|hadoop-node' /etc/hosts || echo '(无)'\n";
    }

    private void executeHostsSync(HadoopCluster node, String script) throws Exception {
        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();

            // 私钥认证
            if (node.getSshPrivateKey() != null && !node.getSshPrivateKey().isEmpty()) {
                jsch.addIdentity("key", node.getSshPrivateKey().getBytes(), null, null);
            }

            session = jsch.getSession(node.getSshUser(), node.getHost(), node.getPort());

            // 密码认证
            if (node.getSshPrivateKey() == null || node.getSshPrivateKey().isEmpty()) {
                session.setPassword(node.getSshPassword());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(30000);
            session.connect();

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(script);
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            channel.connect(10000);

            // 等待执行完成
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            int exitStatus = channel.getExitStatus();
            if (exitStatus != 0) {
                throw new RuntimeException("脚本执行失败，退出码: " + exitStatus);
            }

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    // ==================== 健康检查 ====================

    @Override
    public Map<String, Object> getClusterHealth(String clusterType, Long masterId) {
        Map<String, Object> health = new HashMap<>();
        health.put("clusterType", clusterType);
        health.put("masterId", masterId);

        switch (clusterType.toLowerCase()) {
            case "hadoop":
                return getHadoopClusterHealth(masterId);
            case "jenkins":
                return getJenkinsClusterHealth(masterId);
            default:
                health.put("status", "unknown");
                health.put("message", "不支持的集群类型");
                return health;
        }
    }

    /**
     * 检查单个Hadoop节点的实时运行状态
     * 通过SSH执行jps命令检查Hadoop进程是否运行
     */
    public String checkHadoopNodeRealTimeStatus(HadoopCluster node) {
        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();

            // 私钥认证
            if (node.getSshPrivateKey() != null && !node.getSshPrivateKey().isEmpty()) {
                jsch.addIdentity("key", node.getSshPrivateKey().getBytes(), null, null);
            }

            session = jsch.getSession(node.getSshUser(), node.getHost(), node.getPort());

            // 密码认证
            if (node.getSshPrivateKey() == null || node.getSshPrivateKey().isEmpty()) {
                session.setPassword(node.getSshPassword());
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(10000);
            session.connect();

            channel = (ChannelExec) session.openChannel("exec");

            // 根据节点类型检查不同的进程
            String checkCommand;
            if ("master".equals(node.getNodeType())) {
                // Master节点检查 NameNode 和 ResourceManager
                checkCommand = "jps 2>/dev/null | grep -E 'NameNode|ResourceManager' | wc -l";
            } else {
                // Node节点检查 DataNode 和 NodeManager
                checkCommand = "jps 2>/dev/null | grep -E 'DataNode|NodeManager' | wc -l";
            }

            channel.setCommand(checkCommand);
            channel.setInputStream(null);

            java.io.InputStream in = channel.getInputStream();
            channel.connect(5000);

            StringBuilder output = new StringBuilder();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(100);
            }

            int processCount = 0;
            try {
                processCount = Integer.parseInt(output.toString().trim());
            } catch (NumberFormatException e) {
                // 解析失败
            }

            // 至少有1个关键进程在运行就认为是running状态
            if (processCount >= 1) {
                return "running";
            } else if ("deployed".equals(node.getStatus()) || "running".equals(node.getStatus())) {
                return "stopped";
            } else {
                return node.getStatus();
            }

        } catch (Exception e) {
            logger.warn("检查节点状态失败 {}: {}", node.getHost(), e.getMessage());
            // 连接失败，如果之前是运行状态则标记为stopped
            if ("running".equals(node.getStatus()) || "deployed".equals(node.getStatus())) {
                return "stopped";
            }
            return node.getStatus();
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
     * 刷新Hadoop集群所有节点的实时状态
     */
    public void refreshHadoopClusterStatus(Long masterId) {
        Optional<HadoopCluster> masterOpt = hadoopClusterMapper.findById(masterId);
        if (masterOpt.isEmpty()) {
            return;
        }

        HadoopCluster master = masterOpt.get();
        List<HadoopCluster> nodes = hadoopClusterMapper.findByMasterId(masterId);

        // 检查Master状态
        if ("deployed".equals(master.getStatus()) || "running".equals(master.getStatus()) || "stopped".equals(master.getStatus())) {
            String realStatus = checkHadoopNodeRealTimeStatus(master);
            if (!realStatus.equals(master.getStatus())) {
                logger.info("Master {} 状态变更: {} -> {}", master.getHost(), master.getStatus(), realStatus);
                hadoopClusterMapper.updateStatusOnly(master.getId(), realStatus);
            }
        }

        // 检查所有Node状态
        for (HadoopCluster node : nodes) {
            if ("deployed".equals(node.getStatus()) || "running".equals(node.getStatus()) || "stopped".equals(node.getStatus())) {
                String realStatus = checkHadoopNodeRealTimeStatus(node);
                if (!realStatus.equals(node.getStatus())) {
                    logger.info("Node {} 状态变更: {} -> {}", node.getHost(), node.getStatus(), realStatus);
                    hadoopClusterMapper.updateStatusOnly(node.getId(), realStatus);
                }
            }
        }
    }

    private Map<String, Object> getHadoopClusterHealth(Long masterId) {
        Map<String, Object> health = new HashMap<>();
        health.put("clusterType", "hadoop");
        health.put("masterId", masterId);

        Optional<HadoopCluster> masterOpt = hadoopClusterMapper.findById(masterId);
        if (masterOpt.isEmpty()) {
            health.put("status", "error");
            health.put("message", "Master节点不存在");
            return health;
        }

        HadoopCluster master = masterOpt.get();
        List<HadoopCluster> nodes = hadoopClusterMapper.findByMasterId(masterId);

        long runningNodes = nodes.stream().filter(n -> isRunningStatus(n.getStatus())).count();

        health.put("status", isRunningStatus(master.getStatus()) ? "healthy" : "unhealthy");
        health.put("masterStatus", master.getStatus());
        health.put("masterHost", master.getHost());
        health.put("totalNodes", nodes.size());
        health.put("runningNodes", runningNodes);
        health.put("hadoopVersion", master.getHadoopVersion());

        return health;
    }

    private Map<String, Object> getJenkinsClusterHealth(Long masterId) {
        Map<String, Object> health = new HashMap<>();
        health.put("clusterType", "jenkins");
        health.put("masterId", masterId);

        Optional<JenkinsMaster> masterOpt = jenkinsMasterMapper.findById(masterId);
        if (masterOpt.isEmpty()) {
            health.put("status", "error");
            health.put("message", "Master节点不存在");
            return health;
        }

        JenkinsMaster master = masterOpt.get();
        List<JenkinsNode> nodes = jenkinsNodeMapper.findAll();

        health.put("status", isRunningStatus(master.getStatus()) ? "healthy" : "unhealthy");
        health.put("masterStatus", master.getStatus());
        health.put("masterHost", master.getHost());
        health.put("jenkinsPort", master.getJenkins_port());
        health.put("totalNodes", nodes.size());

        return health;
    }
}
