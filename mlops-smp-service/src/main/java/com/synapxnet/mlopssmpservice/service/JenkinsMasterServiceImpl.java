package com.synapxnet.mlopssmpservice.service;

import com.jcraft.jsch.*;
import com.synapxnet.mlopssmpservice.entity.JenkinsMaster;
import com.synapxnet.mlopssmpservice.entity.JenkinsMasterDeployConfig;
import com.synapxnet.mlopssmpservice.exception.DuplicateEntryException;
import com.synapxnet.mlopssmpservice.exception.EntityNotFoundException;
import com.synapxnet.mlopssmpservice.mapper.JenkinsMasterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Jenkins Master 服务实现类
 */
@Service
public class JenkinsMasterServiceImpl implements JenkinsMasterService {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsMasterServiceImpl.class);

    // AES加密密钥(与JenkinsNodeServiceImpl保持一致)
    private static final String AES_KEY = "XnetMLopsJenkins";

    // SSH超时时间(毫秒)
    private static final int SSH_TIMEOUT = 60000;

    // 部署超时时间(毫秒) - 30分钟
    private static final int DEPLOY_TIMEOUT = 1800000;

    @Autowired
    private JenkinsMasterMapper jenkinsMasterMapper;

    @Autowired
    private ApplicationContext applicationContext;

    // ==================== CRUD操作 ====================

    @Override
    @Transactional
    public JenkinsMaster createMaster(JenkinsMaster master) {
        // 检查名称唯一性
        if (jenkinsMasterMapper.countByName(master.getName(), null) > 0) {
            throw new DuplicateEntryException("Master名称已存在: " + master.getName());
        }

        // 检查主机+Jenkins端口唯一性
        Integer jenkinsPort = master.getJenkins_port() != null ? master.getJenkins_port() : 8080;
        if (jenkinsMasterMapper.countByHostAndJenkinsPort(master.getHost(), jenkinsPort, null) > 0) {
            throw new DuplicateEntryException("该主机的Jenkins端口已被占用: " + master.getHost() + ":" + jenkinsPort);
        }

        // 生成UID
        master.setUid(UUID.randomUUID().toString());

        // 设置默认值
        if (master.getPort() == null) master.setPort(22);
        if (master.getOs_type() == null) master.setOs_type("linux");
        if (master.getJenkins_port() == null) master.setJenkins_port(8080);
        if (master.getJenkins_home() == null) master.setJenkins_home("/var/jenkins_home");
        if (master.getJava_version() == null) master.setJava_version("17");
        if (master.getJava_opts() == null) master.setJava_opts("-Xmx2g -Xms1g");
        if (master.getAdmin_username() == null) master.setAdmin_username("admin");
        if (master.getStatus() == null) master.setStatus("pending");
        if (master.getRegion() == null) master.setRegion("guangzhou");
        if (master.getCpu_cores() == null) master.setCpu_cores(4);
        if (master.getRam_gb() == null) master.setRam_gb(8);
        if (master.getDisk_gb() == null) master.setDisk_gb(100);

        // 加密密码
        if (master.getPassword() != null && !master.getPassword().isEmpty()) {
            master.setEncrypted_password(encryptPassword(master.getPassword()));
        }
        if (master.getAdmin_password() != null && !master.getAdmin_password().isEmpty()) {
            master.setEncrypted_admin_password(encryptPassword(master.getAdmin_password()));
        }

        jenkinsMasterMapper.insert(master);
        logger.info("创建Jenkins Master: {}", master.getName());

        return master;
    }

    @Override
    @Transactional
    public JenkinsMaster updateMaster(Long id, JenkinsMaster master) {
        JenkinsMaster existing = getMasterById(id);

        // 检查名称唯一性
        if (!existing.getName().equals(master.getName())) {
            if (jenkinsMasterMapper.countByName(master.getName(), id) > 0) {
                throw new DuplicateEntryException("Master名称已存在: " + master.getName());
            }
        }

        // 检查主机+Jenkins端口唯一性
        Integer jenkinsPort = master.getJenkins_port() != null ? master.getJenkins_port() : existing.getJenkins_port();
        if (!existing.getHost().equals(master.getHost()) || !existing.getJenkins_port().equals(jenkinsPort)) {
            if (jenkinsMasterMapper.countByHostAndJenkinsPort(master.getHost(), jenkinsPort, id) > 0) {
                throw new DuplicateEntryException("该主机的Jenkins端口已被占用: " + master.getHost() + ":" + jenkinsPort);
            }
        }

        // 更新字段
        master.setId(id);
        master.setUid(existing.getUid());

        // 处理密码更新
        if (master.getPassword() != null && !master.getPassword().isEmpty()) {
            master.setEncrypted_password(encryptPassword(master.getPassword()));
        } else {
            master.setEncrypted_password(existing.getEncrypted_password());
        }

        if (master.getAdmin_password() != null && !master.getAdmin_password().isEmpty()) {
            master.setEncrypted_admin_password(encryptPassword(master.getAdmin_password()));
        } else {
            master.setEncrypted_admin_password(existing.getEncrypted_admin_password());
        }

        jenkinsMasterMapper.update(master);
        logger.info("更新Jenkins Master: {}", master.getName());

        return getMasterById(id);
    }

    @Override
    @Transactional
    public void deleteMaster(Long id) {
        JenkinsMaster master = getMasterById(id);
        jenkinsMasterMapper.deleteById(id);
        logger.info("删除Jenkins Master: {}", master.getName());
    }

    @Override
    public JenkinsMaster getMasterById(Long id) {
        return jenkinsMasterMapper.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Master不存在: " + id));
    }

    @Override
    public JenkinsMaster getMasterByUid(String uid) {
        return jenkinsMasterMapper.findByUid(uid)
                .orElseThrow(() -> new EntityNotFoundException("Master不存在: " + uid));
    }

    @Override
    public List<JenkinsMaster> getAllMasters() {
        return jenkinsMasterMapper.findAll();
    }

    @Override
    public List<JenkinsMaster> getMastersByStatus(String status) {
        return jenkinsMasterMapper.findByStatus(status);
    }

    @Override
    public List<JenkinsMaster> getDeployedMasters() {
        return jenkinsMasterMapper.findDeployedMasters();
    }

    // ==================== SSH连接 ====================

    @Override
    public Map<String, Object> testConnection(JenkinsMaster master) {
        Map<String, Object> result = new HashMap<>();
        Session session = null;

        try {
            String password = master.getPassword();
            if (password == null && master.getEncrypted_password() != null) {
                password = decryptPassword(master.getEncrypted_password());
            }

            JSch jsch = new JSch();
            session = jsch.getSession(master.getUsername(), master.getHost(), master.getPort());
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(SSH_TIMEOUT);

            session.connect();

            // 获取系统信息
            String osInfo = executeCommand(session, "uname -a 2>/dev/null || ver 2>nul || echo 'Unknown'", SSH_TIMEOUT);
            String hostname = executeCommand(session, "hostname", SSH_TIMEOUT);

            // 检查磁盘空间
            String diskInfo = executeCommand(session, "df -h / 2>/dev/null | tail -1 | awk '{print $4}'", SSH_TIMEOUT);

            // 检查内存
            String memInfo = executeCommand(session, "free -m 2>/dev/null | grep Mem | awk '{print $2}'", SSH_TIMEOUT);

            result.put("success", true);
            result.put("message", "连接成功");
            result.put("osInfo", osInfo.trim());
            result.put("hostname", hostname.trim());
            result.put("availableDisk", diskInfo.trim());
            result.put("totalMemoryMb", memInfo.trim());

            // 检测操作系统类型
            String detectedOs = detectOsType(osInfo);
            result.put("detectedOsType", detectedOs);

        } catch (JSchException e) {
            logger.error("SSH连接失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("测试连接异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    // ==================== 部署操作 ====================

    @Override
    @Transactional
    public Map<String, Object> deployMaster(Long masterId, JenkinsMasterDeployConfig config) {
        if (config.getAdminPassword() == null || config.getAdminPassword().isBlank()) {
            throw new IllegalArgumentException("Jenkins 管理员密码不能为空");
        }
        Map<String, Object> result = new HashMap<>();
        JenkinsMaster master = getMasterById(masterId);

        // 更新配置
        if (config.getJenkinsVersion() != null) master.setJenkins_version(config.getJenkinsVersion());
        if (config.getJenkinsPort() != null) master.setJenkins_port(config.getJenkinsPort());
        if (config.getJenkinsHome() != null) master.setJenkins_home(config.getJenkinsHome());
        if (config.getJavaVersion() != null) master.setJava_version(config.getJavaVersion());
        if (config.getJavaOpts() != null) master.setJava_opts(config.getJavaOpts());
        if (config.getAdminUsername() != null) master.setAdmin_username(config.getAdminUsername());
        if (config.getAdminPassword() != null) {
            master.setEncrypted_admin_password(encryptPassword(config.getAdminPassword()));
        }

        master.setStatus("deploying");
        jenkinsMasterMapper.update(master);

        result.put("success", true);
        result.put("message", "部署任务已启动");
        result.put("masterId", masterId);

        // 通过ApplicationContext获取代理对象，确保@Async生效
        JenkinsMasterServiceImpl proxy = applicationContext.getBean(JenkinsMasterServiceImpl.class);
        proxy.deployMasterAsync(masterId, config);

        return result;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deployMasterAsync(Long masterId, JenkinsMasterDeployConfig config) {
        Session session = null;
        StringBuilder deployLog = new StringBuilder();

        JenkinsMaster master = jenkinsMasterMapper.findById(masterId)
                .orElseThrow(() -> new RuntimeException("Master不存在: " + masterId));

        try {
            String password = decryptPassword(master.getEncrypted_password());

            // 建立SSH连接
            JSch jsch = new JSch();
            session = jsch.getSession(master.getUsername(), master.getHost(), master.getPort());
            session.setPassword(password);

            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sshConfig);
            session.setTimeout(SSH_TIMEOUT);
            session.connect();

            deployLog.append("=== 开始部署 Jenkins Master ===\n");
            deployLog.append("时间: ").append(new Date()).append("\n");
            deployLog.append("目标主机: ").append(master.getHost()).append("\n");
            deployLog.append("Jenkins版本: ").append(config.getJenkinsVersion()).append("\n\n");

            // 获取部署脚本（传入主机IP用于配置Jenkins URL）
            String script = getDeployScript(master.getOs_type(), config, master.getHost());

            // 上传脚本
            String scriptPath = "/tmp/jenkins_master_deploy_" + System.currentTimeMillis() + ".sh";
            deployLog.append("上传部署脚本到: ").append(scriptPath).append("\n");
            uploadScript(session, script, scriptPath);

            // 更新日志
            jenkinsMasterMapper.updateStatus(masterId, "deploying", deployLog.toString());

            // 执行脚本
            deployLog.append("\n执行部署脚本...\n");
            deployLog.append("────────────────────────────────────────\n");
            String output = executeCommand(session, "chmod +x " + scriptPath + " && " + scriptPath, DEPLOY_TIMEOUT);
            deployLog.append(output);
            deployLog.append("────────────────────────────────────────\n");

            // 检查部署结果
            if (output.contains("Jenkins Master 安装完成") || output.contains("Installation completed")) {
                master.setStatus("deployed");
                deployLog.append("\n=== 部署成功 ===\n");

                // 尝试获取初始密码
                String initialPassword = getInitialPasswordFromServer(session, master.getJenkins_home());
                if (initialPassword != null && !initialPassword.isEmpty()) {
                    master.setInitial_password(initialPassword);
                    deployLog.append("初始密码: ").append(initialPassword).append("\n");
                }
            } else {
                master.setStatus("failed");
                deployLog.append("\n=== 部署失败 ===\n");
            }

        } catch (Exception e) {
            logger.error("部署失败: {}", e.getMessage(), e);
            master.setStatus("failed");
            deployLog.append("\n=== 部署失败 ===\n");
            deployLog.append("错误: ").append(e.getMessage()).append("\n");
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }

            master.setDeploy_log(deployLog.toString());
            jenkinsMasterMapper.updateStatus(master.getId(), master.getStatus(), deployLog.toString());
            if (master.getInitial_password() != null) {
                jenkinsMasterMapper.updateInitialPassword(master.getId(), master.getInitial_password());
            }
        }
    }

    @Override
    public String getDeployScript(String osType, JenkinsMasterDeployConfig config) {
        // 调用带有 hostIp 参数的重载方法，默认使用空字符串（脚本会自动检测）
        return getDeployScript(osType, config, "");
    }

    /**
     * 获取部署脚本（带主机IP参数）
     * @param osType 操作系统类型
     * @param config 部署配置
     * @param hostIp 主机IP地址（用于配置Jenkins URL）
     * @return 部署脚本内容
     */
    public String getDeployScript(String osType, JenkinsMasterDeployConfig config, String hostIp) {
        try {
            String templatePath;
            switch (osType.toLowerCase()) {
                case "macos":
                    templatePath = "scripts/jenkins-master-macos.sh";
                    break;
                case "windows":
                    templatePath = "scripts/jenkins-master-windows.ps1";
                    break;
                case "linux":
                default:
                    templatePath = "scripts/jenkins-master-linux.sh";
                    break;
            }

            ClassPathResource resource = new ClassPathResource(templatePath);
            String template;
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
                template = new String(bytes, StandardCharsets.UTF_8);
            }

            // 替换参数
            template = template.replace("${JENKINS_VERSION}",
                    config.getJenkinsVersion() != null ? config.getJenkinsVersion() : "2.462.3");
            template = template.replace("${JENKINS_PORT}",
                    String.valueOf(config.getJenkinsPort() != null ? config.getJenkinsPort() : 8080));
            template = template.replace("${JENKINS_HOME}",
                    config.getJenkinsHome() != null ? config.getJenkinsHome() : "/var/jenkins_home");
            template = template.replace("${JAVA_VERSION}",
                    config.getJavaVersion() != null ? config.getJavaVersion() : "17");
            template = template.replace("${JAVA_OPTS}",
                    config.getJavaOpts() != null ? config.getJavaOpts() : "-Xmx2g -Xms1g");
            template = template.replace("${ADMIN_USERNAME}",
                    config.getAdminUsername() != null ? config.getAdminUsername() : "admin");
            template = template.replace("${ADMIN_PASSWORD}",
                    config.getAdminPassword());
            template = template.replace("${ADMIN_EMAIL}",
                    config.getAdminEmail() != null ? config.getAdminEmail() : "admin@localhost");
            template = template.replace("${INSTALL_SUGGESTED_PLUGINS}",
                    config.getInstallSuggestedPlugins() == null || config.getInstallSuggestedPlugins() ? "true" : "false");
            // 传入主机IP地址用于配置Jenkins URL
            template = template.replace("${HOST_IP}",
                    hostIp != null && !hostIp.isEmpty() ? hostIp : "");
            // 时区配置
            template = template.replace("${TIMEZONE}",
                    config.getTimezone() != null ? config.getTimezone() : "Asia/Shanghai");
            
            // 生成凭证配置 Groovy 脚本
            String credentialsScript = generateCredentialsGroovyScript(config);
            template = template.replace("${CREDENTIALS_GROOVY_SCRIPT}", credentialsScript);

            return template;

        } catch (Exception e) {
            throw new RuntimeException("读取部署脚本模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成凭证配置的 Groovy 脚本
     */
    private String generateCredentialsGroovyScript(JenkinsMasterDeployConfig config) {
        StringBuilder script = new StringBuilder();
        
        boolean hasCredentials = (config.getGitCredentials() != null && !config.getGitCredentials().isEmpty()) ||
                                 (config.getHarborCredentials() != null && !config.getHarborCredentials().isEmpty()) ||
                                 (config.getSshCredentials() != null && !config.getSshCredentials().isEmpty());
        
        if (!hasCredentials) {
            return "# 没有配置凭证";
        }
        
        // Git 凭证
        if (config.getGitCredentials() != null) {
            for (JenkinsMasterDeployConfig.GitCredential cred : config.getGitCredentials()) {
                if (cred.getId() != null && !cred.getId().isEmpty() && 
                    cred.getUsername() != null && !cred.getUsername().isEmpty()) {
                    script.append(String.format(
                        "createUsernamePasswordCredential('%s', '%s', '%s', '%s')\n",
                        escapeGroovyString(cred.getId()),
                        escapeGroovyString(cred.getDescription() != null ? cred.getDescription() : ""),
                        escapeGroovyString(cred.getUsername()),
                        escapeGroovyString(cred.getPassword() != null ? cred.getPassword() : "")
                    ));
                }
            }
        }
        
        // Harbor 凭证
        if (config.getHarborCredentials() != null) {
            for (JenkinsMasterDeployConfig.HarborCredential cred : config.getHarborCredentials()) {
                if (cred.getId() != null && !cred.getId().isEmpty() && 
                    cred.getUsername() != null && !cred.getUsername().isEmpty()) {
                    String desc = (cred.getDescription() != null ? cred.getDescription() : "") +
                                  (cred.getUrl() != null ? " (Harbor: " + cred.getUrl() + ")" : "");
                    script.append(String.format(
                        "createUsernamePasswordCredential('%s', '%s', '%s', '%s')\n",
                        escapeGroovyString(cred.getId()),
                        escapeGroovyString(desc),
                        escapeGroovyString(cred.getUsername()),
                        escapeGroovyString(cred.getPassword() != null ? cred.getPassword() : "")
                    ));
                }
            }
        }
        
        // SSH 凭证
        if (config.getSshCredentials() != null) {
            for (JenkinsMasterDeployConfig.SSHCredential cred : config.getSshCredentials()) {
                if (cred.getId() != null && !cred.getId().isEmpty() && 
                    cred.getUsername() != null && !cred.getUsername().isEmpty() &&
                    cred.getPrivateKey() != null && !cred.getPrivateKey().isEmpty()) {
                    // SSH 私钥使用 Base64 编码传递
                    String privateKeyBase64 = Base64.getEncoder().encodeToString(
                        cred.getPrivateKey().getBytes(StandardCharsets.UTF_8));
                    script.append(String.format(
                        "createSshCredential('%s', '%s', '%s', '%s', '%s')\n",
                        escapeGroovyString(cred.getId()),
                        escapeGroovyString(cred.getDescription() != null ? cred.getDescription() : ""),
                        escapeGroovyString(cred.getUsername()),
                        privateKeyBase64,
                        escapeGroovyString(cred.getPassphrase() != null ? cred.getPassphrase() : "")
                    ));
                }
            }
        }
        
        return script.length() > 0 ? script.toString() : "# 没有有效的凭证配置";
    }

    // ==================== 状态管理 ====================

    @Override
    public Map<String, Object> checkMasterStatus(Long masterId) {
        Map<String, Object> result = new HashMap<>();
        JenkinsMaster master = getMasterById(masterId);
        Session session = null;

        try {
            String password = decryptPassword(master.getEncrypted_password());

            JSch jsch = new JSch();
            session = jsch.getSession(master.getUsername(), master.getHost(), master.getPort());
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(SSH_TIMEOUT);
            session.connect();

            // 检查Jenkins进程
            String processCheck = executeCommand(session,
                    "pgrep -f 'jenkins.war' > /dev/null && echo 'running' || echo 'stopped'", SSH_TIMEOUT);
            boolean isRunning = processCheck.trim().equals("running");

            // 检查HTTP端口
            String portCheck = executeCommand(session,
                    "curl -s -o /dev/null -w '%{http_code}' http://localhost:" + master.getJenkins_port() + " 2>/dev/null || echo '000'",
                    SSH_TIMEOUT);
            boolean isResponding = !portCheck.trim().equals("000");

            result.put("success", true);
            result.put("isRunning", isRunning);
            result.put("isResponding", isResponding);
            result.put("status", isRunning ? (isResponding ? "running" : "starting") : "stopped");

            // 更新心跳
            jenkinsMasterMapper.updateHeartbeat(masterId);

        } catch (Exception e) {
            logger.error("检查状态失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("status", "offline");
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> startJenkins(Long masterId) {
        return executeJenkinsCommand(masterId, "start");
    }

    @Override
    public Map<String, Object> stopJenkins(Long masterId) {
        return executeJenkinsCommand(masterId, "stop");
    }

    @Override
    public Map<String, Object> restartJenkins(Long masterId) {
        return executeJenkinsCommand(masterId, "restart");
    }

    private Map<String, Object> executeJenkinsCommand(Long masterId, String action) {
        Map<String, Object> result = new HashMap<>();
        JenkinsMaster master = getMasterById(masterId);
        Session session = null;

        try {
            String password = decryptPassword(master.getEncrypted_password());

            JSch jsch = new JSch();
            session = jsch.getSession(master.getUsername(), master.getHost(), master.getPort());
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(SSH_TIMEOUT);
            session.connect();

            String command = "sudo systemctl " + action + " jenkins";
            String output = executeCommand(session, command, SSH_TIMEOUT);

            result.put("success", true);
            result.put("message", "Jenkins " + action + " 命令已执行");
            result.put("output", output);

        } catch (Exception e) {
            logger.error("执行Jenkins命令失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    @Override
    public String getInitialPassword(Long masterId) {
        JenkinsMaster master = getMasterById(masterId);

        // 如果已经保存了初始密码，直接返回
        if (master.getInitial_password() != null && !master.getInitial_password().isEmpty()) {
            return master.getInitial_password();
        }

        // 从服务器获取
        Session session = null;
        try {
            String password = decryptPassword(master.getEncrypted_password());

            JSch jsch = new JSch();
            session = jsch.getSession(master.getUsername(), master.getHost(), master.getPort());
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(SSH_TIMEOUT);
            session.connect();

            String initialPassword = getInitialPasswordFromServer(session, master.getJenkins_home());

            // 保存到数据库
            if (initialPassword != null && !initialPassword.isEmpty()) {
                jenkinsMasterMapper.updateInitialPassword(masterId, initialPassword);
            }

            return initialPassword;

        } catch (Exception e) {
            logger.error("获取初始密码失败: {}", e.getMessage());
            return null;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private String getInitialPasswordFromServer(Session session, String jenkinsHome) {
        try {
            String passwordFile = jenkinsHome + "/secrets/initialAdminPassword";
            String password = executeCommand(session, "cat " + passwordFile + " 2>/dev/null", SSH_TIMEOUT);
            return password.trim();
        } catch (Exception e) {
            logger.warn("获取初始密码失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 凭证管理 ====================

    @Override
    public Map<String, Object> configureCredentials(Long masterId, JenkinsMasterDeployConfig config) {
        Map<String, Object> result = new HashMap<>();
        JenkinsMaster master = getMasterById(masterId);
        
        try {
            String jenkinsUrl = String.format("http://%s:%d", master.getHost(), master.getJenkins_port());
            String adminUser = master.getAdmin_username();
            String encryptedPassword = master.getEncrypted_admin_password();
            
            if (encryptedPassword == null || encryptedPassword.isEmpty()) {
                result.put("success", false);
                result.put("message", "Jenkins Master管理员密码未配置");
                return result;
            }
            
            String adminPassword = decryptPassword(encryptedPassword);
            
            // 测试Jenkins连接
            if (!testJenkinsConnection(jenkinsUrl, adminUser, adminPassword)) {
                result.put("success", false);
                result.put("message", "无法连接到Jenkins Master: " + jenkinsUrl);
                return result;
            }
            
            List<String> createdCredentials = new ArrayList<>();
            List<String> failedCredentials = new ArrayList<>();
            
            // 1. 创建Git凭证
            if (config.getGitCredentials() != null && !config.getGitCredentials().isEmpty()) {
                for (JenkinsMasterDeployConfig.GitCredential gitCred : config.getGitCredentials()) {
                    boolean success = createUsernamePasswordCredential(
                        jenkinsUrl, adminUser, adminPassword,
                        gitCred.getId(), gitCred.getDescription(),
                        gitCred.getUsername(), gitCred.getPassword()
                    );
                    if (success) {
                        createdCredentials.add("Git: " + gitCred.getId());
                    } else {
                        failedCredentials.add("Git: " + gitCred.getId());
                    }
                }
            }
            
            // 2. 创建Harbor凭证
            if (config.getHarborCredentials() != null && !config.getHarborCredentials().isEmpty()) {
                for (JenkinsMasterDeployConfig.HarborCredential harborCred : config.getHarborCredentials()) {
                    boolean success = createUsernamePasswordCredential(
                        jenkinsUrl, adminUser, adminPassword,
                        harborCred.getId(), harborCred.getDescription() + " (Harbor: " + harborCred.getUrl() + ")",
                        harborCred.getUsername(), harborCred.getPassword()
                    );
                    if (success) {
                        createdCredentials.add("Harbor: " + harborCred.getId());
                    } else {
                        failedCredentials.add("Harbor: " + harborCred.getId());
                    }
                }
            }
            
            // 3. 创建SSH凭证
            if (config.getSshCredentials() != null && !config.getSshCredentials().isEmpty()) {
                for (JenkinsMasterDeployConfig.SSHCredential sshCred : config.getSshCredentials()) {
                    boolean success = createSshCredential(
                        jenkinsUrl, adminUser, adminPassword,
                        sshCred.getId(), sshCred.getDescription(),
                        sshCred.getUsername(), sshCred.getPrivateKey(), sshCred.getPassphrase()
                    );
                    if (success) {
                        createdCredentials.add("SSH: " + sshCred.getId());
                    } else {
                        failedCredentials.add("SSH: " + sshCred.getId());
                    }
                }
            }
            
            // 保存凭证配置到数据库
            saveCredentialsConfig(masterId, config);
            
            // 构建结果
            if (failedCredentials.isEmpty()) {
                result.put("success", true);
                result.put("message", "成功创建 " + createdCredentials.size() + " 个凭证");
                result.put("createdCredentials", createdCredentials);
            } else {
                result.put("success", createdCredentials.size() > 0);
                result.put("message", "创建 " + createdCredentials.size() + " 个凭证，失败 " + failedCredentials.size() + " 个");
                result.put("createdCredentials", createdCredentials);
                result.put("failedCredentials", failedCredentials);
            }
            
        } catch (Exception e) {
            logger.error("配置凭证失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "配置凭证异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 创建Username/Password类型凭证（用于Git、Harbor等）
     */
    private boolean createUsernamePasswordCredential(String jenkinsUrl, String user, String password,
            String credentialId, String description, String credUsername, String credPassword) {
        
        String groovyScript = String.format(
            "import jenkins.model.*\n" +
            "import com.cloudbees.plugins.credentials.*\n" +
            "import com.cloudbees.plugins.credentials.impl.*\n" +
            "import com.cloudbees.plugins.credentials.domains.*\n" +
            "\n" +
            "def jenkins = Jenkins.instance\n" +
            "def domain = Domain.global()\n" +
            "def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()\n" +
            "\n" +
            "// 检查凭证是否已存在\n" +
            "def existingCred = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(\n" +
            "    com.cloudbees.plugins.credentials.common.StandardUsernameCredentials.class,\n" +
            "    jenkins,\n" +
            "    null,\n" +
            "    null\n" +
            ").find { it.id == '%s' }\n" +
            "\n" +
            "if (existingCred != null) {\n" +
            "    println 'Credential already exists: %s'\n" +
            "} else {\n" +
            "    def credential = new UsernamePasswordCredentialsImpl(\n" +
            "        CredentialsScope.GLOBAL,\n" +
            "        '%s',\n" +
            "        '%s',\n" +
            "        '%s',\n" +
            "        '%s'\n" +
            "    )\n" +
            "    store.addCredentials(domain, credential)\n" +
            "    println 'Credential created successfully: %s'\n" +
            "}\n",
            escapeGroovyString(credentialId),
            escapeGroovyString(credentialId),
            escapeGroovyString(credentialId),
            escapeGroovyString(description),
            escapeGroovyString(credUsername),
            escapeGroovyString(credPassword),
            escapeGroovyString(credentialId)
        );
        
        return executeGroovyScript(jenkinsUrl, user, password, groovyScript);
    }
    
    /**
     * 创建SSH凭证
     */
    private boolean createSshCredential(String jenkinsUrl, String user, String password,
            String credentialId, String description, String sshUsername, String privateKey, String passphrase) {
        
        // 将私钥转换为三引号字符串格式
        String escapedPrivateKey = privateKey.replace("\\", "\\\\").replace("$", "\\$");
        
        String groovyScript = String.format(
            "import jenkins.model.*\n" +
            "import com.cloudbees.plugins.credentials.*\n" +
            "import com.cloudbees.plugins.credentials.domains.*\n" +
            "import com.cloudbees.jenkins.plugins.sshcredentials.impl.*\n" +
            "\n" +
            "def jenkins = Jenkins.instance\n" +
            "def domain = Domain.global()\n" +
            "def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()\n" +
            "\n" +
            "// 检查凭证是否已存在\n" +
            "def existingCred = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(\n" +
            "    com.cloudbees.plugins.credentials.common.StandardUsernameCredentials.class,\n" +
            "    jenkins,\n" +
            "    null,\n" +
            "    null\n" +
            ").find { it.id == '%s' }\n" +
            "\n" +
            "if (existingCred != null) {\n" +
            "    println 'SSH Credential already exists: %s'\n" +
            "} else {\n" +
            "    def privateKeySource = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource('''%s''')\n" +
            "    def credential = new BasicSSHUserPrivateKey(\n" +
            "        CredentialsScope.GLOBAL,\n" +
            "        '%s',\n" +
            "        '%s',\n" +
            "        privateKeySource,\n" +
            "        '%s',\n" +
            "        '%s'\n" +
            "    )\n" +
            "    store.addCredentials(domain, credential)\n" +
            "    println 'SSH Credential created successfully: %s'\n" +
            "}\n",
            escapeGroovyString(credentialId),
            escapeGroovyString(credentialId),
            escapedPrivateKey,
            escapeGroovyString(credentialId),
            escapeGroovyString(sshUsername),
            passphrase != null ? escapeGroovyString(passphrase) : "",
            escapeGroovyString(description),
            escapeGroovyString(credentialId)
        );
        
        return executeGroovyScript(jenkinsUrl, user, password, groovyScript);
    }
    
    /**
     * 执行Groovy脚本
     */
    private boolean executeGroovyScript(String jenkinsUrl, String user, String password, String groovyScript) {
        java.net.HttpURLConnection crumbConn = null;
        java.net.HttpURLConnection scriptConn = null;
        
        try {
            String auth = user + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            
            // 获取Crumb和Session Cookie
            String crumbUrl = jenkinsUrl + "/crumbIssuer/api/json";
            java.net.URL url1 = new java.net.URL(crumbUrl);
            crumbConn = (java.net.HttpURLConnection) url1.openConnection();
            crumbConn.setRequestMethod("GET");
            crumbConn.setConnectTimeout(10000);
            crumbConn.setReadTimeout(10000);
            crumbConn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            
            String crumbFieldName = null;
            String crumbValue = null;
            String sessionCookie = null;
            
            if (crumbConn.getResponseCode() == 200) {
                String setCookie = crumbConn.getHeaderField("Set-Cookie");
                if (setCookie != null) {
                    for (String cookie : setCookie.split(";")) {
                        cookie = cookie.trim();
                        if (cookie.startsWith("JSESSIONID")) {
                            sessionCookie = cookie;
                            break;
                        }
                    }
                }
                
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(crumbConn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    String json = response.toString();
                    int crumbIndex = json.indexOf("\"crumb\":\"");
                    int fieldIndex = json.indexOf("\"crumbRequestField\":\"");
                    if (crumbIndex > 0 && fieldIndex > 0) {
                        crumbValue = json.substring(crumbIndex + 9, json.indexOf("\"", crumbIndex + 9));
                        crumbFieldName = json.substring(fieldIndex + 21, json.indexOf("\"", fieldIndex + 21));
                    }
                }
            }
            
            crumbConn.disconnect();
            crumbConn = null;
            
            // 执行Groovy脚本
            String groovyUrl = jenkinsUrl + "/scriptText";
            java.net.URL url2 = new java.net.URL(groovyUrl);
            scriptConn = (java.net.HttpURLConnection) url2.openConnection();
            scriptConn.setRequestMethod("POST");
            scriptConn.setDoOutput(true);
            scriptConn.setConnectTimeout(30000);
            scriptConn.setReadTimeout(30000);
            scriptConn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            scriptConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            if (sessionCookie != null) {
                scriptConn.setRequestProperty("Cookie", sessionCookie);
            }
            if (crumbFieldName != null && crumbValue != null) {
                scriptConn.setRequestProperty(crumbFieldName, crumbValue);
            }
            
            String postData = "script=" + java.net.URLEncoder.encode(groovyScript, "UTF-8");
            try (java.io.OutputStream os = scriptConn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = scriptConn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(scriptConn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    String result = response.toString();
                    logger.info("Groovy脚本执行结果: {}", result);
                    return result.contains("successfully") || result.contains("already exists");
                }
            } else {
                logger.error("Groovy脚本执行失败: HTTP {}", responseCode);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("执行Groovy脚本失败: {}", e.getMessage(), e);
            return false;
        } finally {
            if (crumbConn != null) crumbConn.disconnect();
            if (scriptConn != null) scriptConn.disconnect();
        }
    }
    
    /**
     * 转义Groovy字符串中的特殊字符
     */
    private String escapeGroovyString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
    
    /**
     * 保存凭证配置到数据库
     */
    private void saveCredentialsConfig(Long masterId, JenkinsMasterDeployConfig config) {
        try {
            // 构建凭证配置JSON（不包含敏感信息）
            Map<String, Object> credConfig = new HashMap<>();
            
            if (config.getGitCredentials() != null) {
                List<Map<String, String>> gitCreds = new ArrayList<>();
                for (JenkinsMasterDeployConfig.GitCredential cred : config.getGitCredentials()) {
                    Map<String, String> credMap = new HashMap<>();
                    credMap.put("id", cred.getId());
                    credMap.put("description", cred.getDescription());
                    credMap.put("username", cred.getUsername());
                    gitCreds.add(credMap);
                }
                credConfig.put("gitCredentials", gitCreds);
            }
            
            if (config.getHarborCredentials() != null) {
                List<Map<String, String>> harborCreds = new ArrayList<>();
                for (JenkinsMasterDeployConfig.HarborCredential cred : config.getHarborCredentials()) {
                    Map<String, String> credMap = new HashMap<>();
                    credMap.put("id", cred.getId());
                    credMap.put("description", cred.getDescription());
                    credMap.put("url", cred.getUrl());
                    credMap.put("username", cred.getUsername());
                    harborCreds.add(credMap);
                }
                credConfig.put("harborCredentials", harborCreds);
            }
            
            if (config.getSshCredentials() != null) {
                List<Map<String, String>> sshCreds = new ArrayList<>();
                for (JenkinsMasterDeployConfig.SSHCredential cred : config.getSshCredentials()) {
                    Map<String, String> credMap = new HashMap<>();
                    credMap.put("id", cred.getId());
                    credMap.put("description", cred.getDescription());
                    credMap.put("username", cred.getUsername());
                    sshCreds.add(credMap);
                }
                credConfig.put("sshCredentials", sshCreds);
            }
            
            // 序列化为JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String credConfigJson = mapper.writeValueAsString(credConfig);
            
            // 更新数据库
            jenkinsMasterMapper.updateCredentialsConfig(masterId, credConfigJson);
            logger.info("凭证配置已保存到数据库, masterId: {}", masterId);
            
        } catch (Exception e) {
            logger.error("保存凭证配置失败: {}", e.getMessage(), e);
        }
    }

    // ==================== Node管理 ====================

    @Override
    public Map<String, Object> createNodeOnMaster(Long masterId, String nodeName, String workDir, String labels) {
        Map<String, Object> result = new HashMap<>();
        JenkinsMaster master = getMasterById(masterId);

        try {
            String jenkinsUrl = String.format("http://%s:%d", master.getHost(), master.getJenkins_port());
            String adminUser = master.getAdmin_username();
            String encryptedPassword = master.getEncrypted_admin_password();

            logger.info("创建Node: masterId={}, nodeName={}, jenkinsUrl={}, adminUser={}",
                    masterId, nodeName, jenkinsUrl, adminUser);

            // 检查管理员密码是否配置
            if (encryptedPassword == null || encryptedPassword.isEmpty()) {
                result.put("success", false);
                result.put("message", "Jenkins Master管理员密码未配置，请先部署Master或配置管理员密码");
                result.put("nodeName", nodeName);
                return result;
            }

            String adminPassword = decryptPassword(encryptedPassword);

            // 先测试Jenkins连接
            if (!testJenkinsConnection(jenkinsUrl, adminUser, adminPassword)) {
                result.put("success", false);
                result.put("message", "无法连接到Jenkins Master: " + jenkinsUrl + "，请确保Jenkins已启动且凭证正确");
                result.put("nodeName", nodeName);
                return result;
            }

            // 构建Node配置JSON
            String nodeJson = buildNodeConfigJson(nodeName, workDir, labels);
            logger.debug("Node配置JSON: {}", nodeJson);

            // 调用Jenkins API创建Node
            boolean created = createJenkinsNode(jenkinsUrl, adminUser, adminPassword, nodeName, nodeJson);

            if (created) {
                // 等待一小段时间让Jenkins处理
                Thread.sleep(1000);

                // 获取Node的Secret
                String secret = fetchNodeSecret(jenkinsUrl, adminUser, adminPassword, nodeName);

                result.put("success", true);
                result.put("message", "Node创建成功");
                result.put("nodeName", nodeName);
                result.put("secret", secret);
                result.put("jenkinsUrl", jenkinsUrl);

                if (secret == null || secret.isEmpty()) {
                    result.put("message", "Node创建成功，但获取Secret失败，请在Jenkins页面手动获取");
                }
            } else {
                result.put("success", false);
                result.put("message", "Node创建失败，请检查Jenkins日志");
                result.put("nodeName", nodeName);
                result.put("jenkinsUrl", jenkinsUrl);
            }

        } catch (Exception e) {
            logger.error("创建Node失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "创建Node异常: " + e.getMessage());
            result.put("nodeName", nodeName);
        }

        return result;
    }

    /**
     * 测试Jenkins连接
     */
    private boolean testJenkinsConnection(String jenkinsUrl, String user, String password) {
        java.net.HttpURLConnection conn = null;
        try {
            String testUrl = jenkinsUrl + "/api/json";
            java.net.URL url = new java.net.URL(testUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String auth = user + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            int code = conn.getResponseCode();
            logger.info("测试Jenkins连接: {} -> {}", testUrl, code);
            return code == 200;
        } catch (Exception e) {
            logger.error("测试Jenkins连接失败: {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public String getNodeSecret(Long masterId, String nodeName) {
        JenkinsMaster master = getMasterById(masterId);

        try {
            String jenkinsUrl = String.format("http://%s:%d", master.getHost(), master.getJenkins_port());
            String adminUser = master.getAdmin_username();
            String adminPassword = decryptPassword(master.getEncrypted_admin_password());

            return fetchNodeSecret(jenkinsUrl, adminUser, adminPassword, nodeName);

        } catch (Exception e) {
            logger.error("获取Node Secret失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建Node配置JSON
     */
    private String buildNodeConfigJson(String nodeName, String workDir, String labels) {
        // Jenkins JNLP Agent Node配置
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"name\": \"").append(nodeName).append("\",");
        json.append("\"nodeDescription\": \"Auto-created by MLOps SMP\",");
        json.append("\"numExecutors\": 2,");
        json.append("\"remoteFS\": \"").append(workDir).append("\",");
        json.append("\"labelString\": \"").append(labels != null ? labels : "").append("\",");
        json.append("\"mode\": \"NORMAL\",");
        json.append("\"retentionStrategy\": {\"stapler-class\": \"hudson.slaves.RetentionStrategy$Always\"},");
        json.append("\"nodeProperties\": {\"stapler-class-bag\": \"true\"},");
        json.append("\"launcher\": {\"stapler-class\": \"hudson.slaves.JNLPLauncher\", \"workDirSettings\": {\"disabled\": false, \"internalDir\": \"remoting\", \"failIfWorkDirIsMissing\": false}}");
        json.append("}");
        return json.toString();
    }

    /**
     * 通过Jenkins API创建Node
     * 使用 Groovy 脚本方式创建，避免 CSRF/Crumb 问题
     */
    private boolean createJenkinsNode(String jenkinsUrl, String user, String password, String nodeName, String nodeJson) {
        // 先检查Node是否已存在
        if (checkNodeExists(jenkinsUrl, user, password, nodeName)) {
            logger.info("Node {} 已存在，跳过创建", nodeName);
            return true;
        }

        // 使用Groovy脚本创建Node（更可靠的方式）
        return createNodeViaGroovy(jenkinsUrl, user, password, nodeName, nodeJson);
    }

    /**
     * 通过Groovy脚本创建Node
     * 使用同一连接会话获取Crumb和执行脚本，确保CSRF验证通过
     */
    private boolean createNodeViaGroovy(String jenkinsUrl, String user, String password, String nodeName, String nodeJson) {
        java.net.HttpURLConnection crumbConn = null;
        java.net.HttpURLConnection scriptConn = null;

        try {
            // Basic认证头
            String auth = user + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            // ============ 步骤1: 获取Crumb和Session Cookie ============
            String crumbUrl = jenkinsUrl + "/crumbIssuer/api/json";
            java.net.URL url1 = new java.net.URL(crumbUrl);
            crumbConn = (java.net.HttpURLConnection) url1.openConnection();
            crumbConn.setRequestMethod("GET");
            crumbConn.setConnectTimeout(10000);
            crumbConn.setReadTimeout(10000);
            crumbConn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            String crumbFieldName = null;
            String crumbValue = null;
            String sessionCookie = null;

            int crumbResponseCode = crumbConn.getResponseCode();
            logger.info("获取Crumb响应码: {}", crumbResponseCode);

            if (crumbResponseCode == 200) {
                // 获取Session Cookie（关键！）
                String setCookie = crumbConn.getHeaderField("Set-Cookie");
                if (setCookie != null) {
                    // 提取JSESSIONID
                    for (String cookie : setCookie.split(";")) {
                        cookie = cookie.trim();
                        if (cookie.startsWith("JSESSIONID")) {
                            sessionCookie = cookie;
                            break;
                        }
                    }
                }
                logger.info("Session Cookie: {}", sessionCookie != null ? sessionCookie.substring(0, Math.min(20, sessionCookie.length())) + "..." : "null");

                // 解析Crumb JSON
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(crumbConn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    String json = response.toString();
                    // 解析: {"_class":"...","crumb":"xxx","crumbRequestField":"Jenkins-Crumb"}
                    int crumbIndex = json.indexOf("\"crumb\":\"");
                    int fieldIndex = json.indexOf("\"crumbRequestField\":\"");
                    if (crumbIndex > 0 && fieldIndex > 0) {
                        crumbValue = json.substring(crumbIndex + 9, json.indexOf("\"", crumbIndex + 9));
                        crumbFieldName = json.substring(fieldIndex + 21, json.indexOf("\"", fieldIndex + 21));
                    }
                }
                logger.info("Crumb Field: {}, Value: {}...", crumbFieldName,
                        crumbValue != null ? crumbValue.substring(0, Math.min(8, crumbValue.length())) : "null");
            }

            crumbConn.disconnect();
            crumbConn = null;

            // ============ 步骤2: 解析Node配置 ============
            String workDir = "/opt/jenkins-agent";
            String labels = "";

            if (nodeJson.contains("\"remoteFS\":")) {
                int start = nodeJson.indexOf("\"remoteFS\":\"") + 12;
                int end = nodeJson.indexOf("\"", start);
                if (end > start) {
                    workDir = nodeJson.substring(start, end);
                }
            }
            if (nodeJson.contains("\"labelString\":")) {
                int start = nodeJson.indexOf("\"labelString\":\"") + 15;
                int end = nodeJson.indexOf("\"", start);
                if (end > start) {
                    labels = nodeJson.substring(start, end);
                }
            }

            // ============ 步骤3: 构建Groovy脚本 ============
            String groovyScript = String.format(
                "import jenkins.model.*\n" +
                "import hudson.model.*\n" +
                "import hudson.slaves.*\n" +
                "\n" +
                "def jenkins = Jenkins.instance\n" +
                "def nodeName = '%s'\n" +
                "def remoteFS = '%s'\n" +
                "def labels = '%s'\n" +
                "\n" +
                "// 检查是否已存在\n" +
                "if (jenkins.getNode(nodeName) != null) {\n" +
                "    println 'Node already exists: ' + nodeName\n" +
                "    return\n" +
                "}\n" +
                "\n" +
                "// 创建JNLP Launcher\n" +
                "def launcher = new JNLPLauncher(true)\n" +
                "\n" +
                "// 创建Node\n" +
                "def node = new DumbSlave(\n" +
                "    nodeName,\n" +
                "    remoteFS,\n" +
                "    launcher\n" +
                ")\n" +
                "node.setNumExecutors(2)\n" +
                "node.setMode(Node.Mode.NORMAL)\n" +
                "node.setLabelString(labels)\n" +
                "node.setRetentionStrategy(new RetentionStrategy.Always())\n" +
                "\n" +
                "jenkins.addNode(node)\n" +
                "println 'Node created successfully: ' + nodeName\n",
                nodeName.replace("'", "\\'"),
                workDir.replace("'", "\\'"),
                labels.replace("'", "\\'")
            );

            // ============ 步骤4: 执行Groovy脚本（带Session Cookie） ============
            String groovyUrl = jenkinsUrl + "/scriptText";
            java.net.URL url2 = new java.net.URL(groovyUrl);
            scriptConn = (java.net.HttpURLConnection) url2.openConnection();
            scriptConn.setRequestMethod("POST");
            scriptConn.setDoOutput(true);
            scriptConn.setConnectTimeout(30000);
            scriptConn.setReadTimeout(30000);

            // Basic认证
            scriptConn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            scriptConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // 添加Session Cookie（关键！确保与获取Crumb时使用同一会话）
            if (sessionCookie != null) {
                scriptConn.setRequestProperty("Cookie", sessionCookie);
                logger.info("已设置Session Cookie");
            }

            // 添加CSRF Crumb头
            if (crumbFieldName != null && crumbValue != null) {
                scriptConn.setRequestProperty(crumbFieldName, crumbValue);
                logger.info("已添加CSRF头: {}={}...", crumbFieldName, crumbValue.substring(0, Math.min(8, crumbValue.length())));
            } else {
                logger.warn("未能获取Jenkins Crumb，请求可能会失败");
            }

            // 发送Groovy脚本
            String postData = "script=" + java.net.URLEncoder.encode(groovyScript, "UTF-8");
            try (java.io.OutputStream os = scriptConn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = scriptConn.getResponseCode();
            logger.info("Groovy创建Node响应码: {}", responseCode);

            if (responseCode == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(scriptConn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    String result = response.toString();
                    logger.info("Groovy脚本执行结果: {}", result);

                    // 检查是否成功
                    if (result.contains("Node created successfully") || result.contains("Node already exists")) {
                        return true;
                    }
                    // 检查是否有错误
                    if (result.contains("Exception") || result.contains("Error")) {
                        logger.error("Groovy脚本执行错误: {}", result);
                        return false;
                    }
                    // 空结果也可能表示成功
                    return true;
                }
            } else {
                // 读取错误响应
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                                scriptConn.getErrorStream() != null ? scriptConn.getErrorStream() : scriptConn.getInputStream(),
                                StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    logger.error("Groovy创建Node失败: HTTP {}, 响应: {}", responseCode,
                            response.toString().substring(0, Math.min(500, response.length())));
                } catch (Exception e) {
                    logger.error("读取错误响应失败: {}", e.getMessage());
                }
                return false;
            }

        } catch (Exception e) {
            logger.error("通过Groovy创建Node失败: {}", e.getMessage(), e);
            return false;
        } finally {
            if (crumbConn != null) {
                crumbConn.disconnect();
            }
            if (scriptConn != null) {
                scriptConn.disconnect();
            }
        }
    }

    /**
     * 检查Node是否已存在
     */
    private boolean checkNodeExists(String jenkinsUrl, String user, String password, String nodeName) {
        java.net.HttpURLConnection conn = null;
        try {
            String checkUrl = jenkinsUrl + "/computer/" + java.net.URLEncoder.encode(nodeName, "UTF-8") + "/api/json";
            java.net.URL url = new java.net.URL(checkUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String auth = user + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            return conn.getResponseCode() == 200;

        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 获取Jenkins CSRF Crumb
     */
    private String getJenkinsCrumb(String jenkinsUrl, String user, String password) {
        java.net.HttpURLConnection conn = null;
        try {
            String crumbUrl = jenkinsUrl + "/crumbIssuer/api/json";
            java.net.URL url = new java.net.URL(crumbUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String auth = user + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            if (conn.getResponseCode() == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    // 解析JSON获取crumb
                    String json = response.toString();
                    // 简单解析: {"_class":"...","crumb":"xxx","crumbRequestField":"Jenkins-Crumb"}
                    int crumbIndex = json.indexOf("\"crumb\":\"");
                    int fieldIndex = json.indexOf("\"crumbRequestField\":\"");
                    if (crumbIndex > 0 && fieldIndex > 0) {
                        String crumbValue = json.substring(crumbIndex + 9, json.indexOf("\"", crumbIndex + 9));
                        String fieldName = json.substring(fieldIndex + 21, json.indexOf("\"", fieldIndex + 21));
                        return fieldName + ":" + crumbValue;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("获取Jenkins Crumb失败: {}", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    /**
     * 获取Node的Agent Secret
     */
    private String fetchNodeSecret(String jenkinsUrl, String user, String password, String nodeName) {
        java.net.HttpURLConnection conn = null;
        try {
            // 方法1: 通过 jnlpMac API 获取
            String secretUrl = jenkinsUrl + "/computer/" + java.net.URLEncoder.encode(nodeName, "UTF-8") + "/slave-agent.jnlp";
            java.net.URL url = new java.net.URL(secretUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            String auth = user + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            if (conn.getResponseCode() == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    // 从JNLP XML中提取Secret
                    // 格式: <argument>secret_value</argument>
                    String content = response.toString();
                    // 查找包含secret的argument标签(通常是第三个argument)
                    int secretArgStart = content.indexOf("<argument>", content.indexOf("<argument>", content.indexOf("<argument>") + 1) + 1);
                    if (secretArgStart > 0) {
                        int secretArgEnd = content.indexOf("</argument>", secretArgStart);
                        if (secretArgEnd > secretArgStart) {
                            String secret = content.substring(secretArgStart + 10, secretArgEnd);
                            // 验证是否是有效的secret格式(64位十六进制)
                            if (secret.matches("[a-f0-9]{64}")) {
                                return secret;
                            }
                        }
                    }

                    // 方法2: 尝试解析所有argument
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<argument>([a-f0-9]{64})</argument>");
                    java.util.regex.Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }

            // 方法3: 通过Groovy脚本获取(如果上面方法失败)
            return fetchNodeSecretViaGroovy(jenkinsUrl, user, password, nodeName);

        } catch (Exception e) {
            logger.error("获取Node Secret失败: {}", e.getMessage(), e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 通过Groovy脚本获取Node Secret
     * 使用同一会话获取Crumb和执行脚本，确保CSRF验证通过
     */
    private String fetchNodeSecretViaGroovy(String jenkinsUrl, String user, String password, String nodeName) {
        java.net.HttpURLConnection crumbConn = null;
        java.net.HttpURLConnection scriptConn = null;

        try {
            // Basic认证头
            String auth = user + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            // ============ 步骤1: 获取Crumb和Session Cookie ============
            String crumbUrl = jenkinsUrl + "/crumbIssuer/api/json";
            java.net.URL url1 = new java.net.URL(crumbUrl);
            crumbConn = (java.net.HttpURLConnection) url1.openConnection();
            crumbConn.setRequestMethod("GET");
            crumbConn.setConnectTimeout(10000);
            crumbConn.setReadTimeout(10000);
            crumbConn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            String crumbFieldName = null;
            String crumbValue = null;
            String sessionCookie = null;

            if (crumbConn.getResponseCode() == 200) {
                // 获取Session Cookie
                String setCookie = crumbConn.getHeaderField("Set-Cookie");
                if (setCookie != null) {
                    for (String cookie : setCookie.split(";")) {
                        cookie = cookie.trim();
                        if (cookie.startsWith("JSESSIONID")) {
                            sessionCookie = cookie;
                            break;
                        }
                    }
                }

                // 解析Crumb JSON
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(crumbConn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    String json = response.toString();
                    int crumbIndex = json.indexOf("\"crumb\":\"");
                    int fieldIndex = json.indexOf("\"crumbRequestField\":\"");
                    if (crumbIndex > 0 && fieldIndex > 0) {
                        crumbValue = json.substring(crumbIndex + 9, json.indexOf("\"", crumbIndex + 9));
                        crumbFieldName = json.substring(fieldIndex + 21, json.indexOf("\"", fieldIndex + 21));
                    }
                }
            }

            crumbConn.disconnect();
            crumbConn = null;

            // ============ 步骤2: 执行Groovy脚本（带Session Cookie） ============
            String groovyUrl = jenkinsUrl + "/scriptText";
            java.net.URL url2 = new java.net.URL(groovyUrl);
            scriptConn = (java.net.HttpURLConnection) url2.openConnection();
            scriptConn.setRequestMethod("POST");
            scriptConn.setDoOutput(true);
            scriptConn.setConnectTimeout(30000);
            scriptConn.setReadTimeout(30000);

            scriptConn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            scriptConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // 添加Session Cookie
            if (sessionCookie != null) {
                scriptConn.setRequestProperty("Cookie", sessionCookie);
            }

            // 添加CSRF Crumb头
            if (crumbFieldName != null && crumbValue != null) {
                scriptConn.setRequestProperty(crumbFieldName, crumbValue);
            }

            // Groovy脚本获取Secret
            String script = "println(jenkins.model.Jenkins.instance.getComputer('" + nodeName + "')?.getJnlpMac())";
            String postData = "script=" + java.net.URLEncoder.encode(script, "UTF-8");

            try (java.io.OutputStream os = scriptConn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            if (scriptConn.getResponseCode() == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(scriptConn.getInputStream(), StandardCharsets.UTF_8))) {
                    String secret = reader.readLine();
                    if (secret != null && secret.matches("[a-f0-9]{64}")) {
                        return secret.trim();
                    }
                }
            }

        } catch (Exception e) {
            logger.error("通过Groovy获取Secret失败: {}", e.getMessage());
        } finally {
            if (crumbConn != null) {
                crumbConn.disconnect();
            }
            if (scriptConn != null) {
                scriptConn.disconnect();
            }
        }
        return null;
    }

    // ==================== 卸载操作 ====================

    @Override
    public Map<String, Object> uninstallJenkins(Long masterId) {
        Map<String, Object> result = new HashMap<>();
        JenkinsMaster master = getMasterById(masterId);
        Session session = null;

        try {
            String password = decryptPassword(master.getEncrypted_password());

            JSch jsch = new JSch();
            session = jsch.getSession(master.getUsername(), master.getHost(), master.getPort());
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(SSH_TIMEOUT);
            session.connect();

            StringBuilder output = new StringBuilder();

            // 停止服务
            output.append(executeCommand(session, "sudo systemctl stop jenkins 2>/dev/null || true", SSH_TIMEOUT));
            output.append(executeCommand(session, "sudo systemctl disable jenkins 2>/dev/null || true", SSH_TIMEOUT));

            // 删除服务文件
            output.append(executeCommand(session, "sudo rm -f /etc/systemd/system/jenkins.service", SSH_TIMEOUT));
            output.append(executeCommand(session, "sudo systemctl daemon-reload", SSH_TIMEOUT));

            // 删除Jenkins文件
            output.append(executeCommand(session, "sudo rm -rf /opt/jenkins", SSH_TIMEOUT));
            output.append(executeCommand(session, "sudo rm -rf " + master.getJenkins_home(), SSH_TIMEOUT));

            result.put("success", true);
            result.put("message", "Jenkins已卸载");
            result.put("output", output.toString());

            // 更新状态
            jenkinsMasterMapper.updateStatus(masterId, "pending", "Jenkins已卸载\n" + output);

        } catch (Exception e) {
            logger.error("卸载Jenkins失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    // ==================== 辅助方法 ====================

    private String executeCommand(Session session, String command, int timeout) {
        ChannelExec channel = null;
        StringBuilder output = new StringBuilder();

        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();

            channel.connect(timeout);

            byte[] buffer = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int len = in.read(buffer);
                    if (len < 0) break;
                    output.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
                }
                while (err.available() > 0) {
                    int len = err.read(buffer);
                    if (len < 0) break;
                    output.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0 || err.available() > 0) continue;
                    break;
                }
                Thread.sleep(100);
            }

        } catch (Exception e) {
            output.append("Error: ").append(e.getMessage());
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }

        return output.toString();
    }

    private void uploadScript(Session session, String content, String remotePath) throws Exception {
        ChannelSftp sftp = null;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            String unixContent = content.replace("\r\n", "\n").replace("\r", "\n");

            try (ByteArrayInputStream bis = new ByteArrayInputStream(
                    unixContent.getBytes(StandardCharsets.UTF_8))) {
                sftp.put(bis, remotePath);
            }
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
        }
    }

    private String detectOsType(String osInfo) {
        String info = osInfo.toLowerCase();
        if (info.contains("darwin") || info.contains("macos")) {
            return "macos";
        } else if (info.contains("windows") || info.contains("microsoft")) {
            return "windows";
        } else {
            return "linux";
        }
    }

    private String encryptPassword(String password) {
        try {
            SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    private String decryptPassword(String encryptedPassword) {
        try {
            SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("密码解密失败", e);
        }
    }
}
