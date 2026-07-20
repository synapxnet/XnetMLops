package com.synapxnet.mlopsmepservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsmepservice.entity.OpenClawInstance;
import com.synapxnet.mlopsmepservice.entity.ApiKey;
import com.synapxnet.mlopsmepservice.mapper.OpenClawInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenClawService {

    private final OpenClawInstanceMapper instanceMapper;
    private final LLMServiceService llmServiceService;
    private final ApiKeyService apiKeyService;
    private final SshRemoteService sshRemoteService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // 加密密钥（生产环境应从配置或密钥管理服务获取）
    @Value("${openclaw.encryption.key:XnetMLops2026Key}")
    private String encryptionKey;

    // 存储正在运行的本地进程（仅本地模式使用）
    private final ConcurrentHashMap<Long, Process> runningProcesses = new ConcurrentHashMap<>();

    // ==================== 加密解密 ====================

    public String encryptApiKey(String plainKey) {
        if (plainKey == null || plainKey.isEmpty()) {
            return null;
        }
        try {
            String key = padKey(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密API密钥失败: {}", e.getMessage());
            return null;
        }
    }

    public String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            return null;
        }
        try {
            String key = padKey(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密API密钥失败: {}", e.getMessage());
            return null;
        }
    }

    private String padKey(String key) {
        if (key.length() < 16) {
            return String.format("%-16s", key).substring(0, 16);
        } else if (key.length() < 24) {
            return String.format("%-24s", key).substring(0, 24);
        } else {
            return String.format("%-32s", key).substring(0, 32);
        }
    }

    // ==================== CRUD ====================

    public List<OpenClawInstance> findAll() {
        return instanceMapper.findAll();
    }

    public OpenClawInstance findById(Long id) {
        return instanceMapper.findById(id);
    }

    public OpenClawInstance findByUid(String uid) {
        return instanceMapper.findByUid(uid);
    }

    public OpenClawInstance create(OpenClawInstance instance) {
        instance.setUid("OPENCLAW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        instance.setStatus("stopped");
        instance.setGatewayPort(instance.getGatewayPort() != null ? instance.getGatewayPort() : 18789);
        instance.setCreatedAt(LocalDateTime.now());

        boolean isRemote = instance.getWorkstationId() != null;

        if (isRemote) {
            // 远程工作站：获取凭证确定远程用户home目录，设置远程路径
            try {
                Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());
                String sshUser = (String) creds.get("sshUser");
                String remoteHome = "root".equals(sshUser) ? "/root" : "/home/" + sshUser;
                String basePath = remoteHome + "/.openclaw/instances/" + instance.getUid();
                instance.setWorkspacePath(basePath + "/workspace");
                instance.setConfigPath(basePath + "/config");
                instance.setLogsPath(basePath + "/logs");

                // 通过SSH在远程创建目录
                String mkdirCmd = String.format("mkdir -p %s/workspace %s/config %s/logs", basePath, basePath, basePath);
                sshRemoteService.executeCommand(creds, mkdirCmd);
                log.info("远程目录创建成功: {}", basePath);
            } catch (Exception e) {
                log.error("远程创建目录失败: {}", e.getMessage(), e);
                // 回退到本地路径，不阻塞创建
                setLocalPaths(instance);
                createLocalDirectories(instance);
            }
        } else {
            // 本地模式
            setLocalPaths(instance);
            createLocalDirectories(instance);
        }

        instanceMapper.insert(instance);
        return instance;
    }

    public OpenClawInstance update(OpenClawInstance instance) {
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.update(instance);
        return instanceMapper.findById(instance.getId());
    }

    public void delete(Long id) {
        OpenClawInstance instance = instanceMapper.findById(id);
        if (instance != null) {
            if ("running".equals(instance.getStatus())) {
                stop(id);
            }
            instanceMapper.deleteById(id);
        }
    }

    // ==================== 启动/停止/重启 ====================

    public Map<String, Object> start(Long id) {
        OpenClawInstance instance = instanceMapper.findById(id);
        if (instance == null) {
            return Map.of("success", false, "message", "实例不存在");
        }

        if ("running".equals(instance.getStatus())) {
            log.warn("实例 {} 已在运行中，跳过启动", instance.getUid());
            return Map.of("success", false, "message", "实例已在运行中");
        }

        log.info("开始启动实例 {} [mode={}, workstationId={}, host={}]",
                instance.getUid(), instance.getDeployMode(),
                instance.getWorkstationId(), instance.getGatewayHost());

        try {
            instanceMapper.updateStatus(id, "starting");

            // 生成并部署配置文件
            generateAndDeployConfig(instance);

            // 根据部署模式启动
            Map<String, Object> result;
            switch (instance.getDeployMode()) {
                case "docker":
                    result = startWithDocker(instance);
                    break;
                case "npm":
                    result = startWithNpm(instance);
                    break;
                case "source":
                    result = startFromSource(instance);
                    break;
                default:
                    result = Map.of("success", false, "message", "不支持的部署模式: " + instance.getDeployMode());
            }

            log.info("实例 {} 启动结果: {}", instance.getUid(), result);
            return result;
        } catch (Exception e) {
            log.error("启动OpenClaw实例失败: {}", e.getMessage(), e);
            instanceMapper.updateRuntime(id, "error", null, null, e.getMessage());
            return Map.of("success", false, "message", "启动失败: " + e.getMessage());
        }
    }

    public Map<String, Object> stop(Long id) {
        OpenClawInstance instance = instanceMapper.findById(id);
        if (instance == null) {
            return Map.of("success", false, "message", "实例不存在");
        }

        try {
            boolean isRemote = instance.getWorkstationId() != null;

            if (isRemote) {
                Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());

                if ("docker".equals(instance.getDeployMode()) && instance.getContainerName() != null) {
                    // 远程Docker停止
                    String cmd = String.format("docker stop %s && docker rm %s",
                            instance.getContainerName(), instance.getContainerName());
                    sshRemoteService.executeCommand(creds, cmd);
                } else {
                    // 远程进程停止：优先使用PID文件，回退到存储的processId
                    StringBuilder cmd = new StringBuilder();
                    if (instance.getLogsPath() != null) {
                        cmd.append(String.format(
                            "if [ -f '%s/gateway.pid' ]; then PID=$(cat '%s/gateway.pid'); kill $PID 2>/dev/null; sleep 1; kill -9 $PID 2>/dev/null; rm -f '%s/gateway.pid'; fi; ",
                            instance.getLogsPath(), instance.getLogsPath(), instance.getLogsPath()));
                    }
                    if (instance.getProcessId() != null) {
                        cmd.append(String.format("kill %s 2>/dev/null; sleep 1; kill -9 %s 2>/dev/null; ",
                            instance.getProcessId(), instance.getProcessId()));
                    }
                    cmd.append("echo done");
                    sshRemoteService.executeCommand(creds, cmd.toString());
                }
            } else {
                // 本地进程停止
                Process process = runningProcesses.remove(id);
                if (process != null && process.isAlive()) {
                    process.destroy();
                    process.waitFor(10, TimeUnit.SECONDS);
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                }

                if ("docker".equals(instance.getDeployMode()) && instance.getContainerName() != null) {
                    stopLocalDockerContainer(instance.getContainerName());
                }
            }

            instanceMapper.updateRuntime(id, "stopped", null, null, null);
            return Map.of("success", true, "message", "停止成功");
        } catch (Exception e) {
            log.error("停止OpenClaw实例失败: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "停止失败: " + e.getMessage());
        }
    }

    public Map<String, Object> restart(Long id) {
        OpenClawInstance instance = instanceMapper.findById(id);
        if (instance == null) {
            return Map.of("success", false, "message", "实例不存在");
        }

        // 对于远程源码模式，使用轻量重启（跳过环境安装和源码构建）
        boolean isRemote = instance.getWorkstationId() != null;
        if (isRemote && "source".equals(instance.getDeployMode())) {
            return restartQuick(id);
        }

        // 其他模式使用常规重启
        stop(id);
        return start(id);
    }

    /**
     * 轻量重启：仅停止进程、重新生成配置并启动Gateway
     * 跳过环境安装(Node.js/pnpm)、源码克隆/更新和依赖构建
     */
    public Map<String, Object> restartQuick(Long id) {
        OpenClawInstance instance = instanceMapper.findById(id);
        if (instance == null) {
            return Map.of("success", false, "message", "实例不存在");
        }

        log.info("轻量重启实例 {} [跳过环境安装和源码构建]", instance.getUid());

        try {
            instanceMapper.updateStatus(id, "starting");

            // 重新生成配置文件
            generateAndDeployConfig(instance);

            boolean isRemote = instance.getWorkstationId() != null;

            if (isRemote && "source".equals(instance.getDeployMode())) {
                return restartRemoteSourceQuick(instance);
            } else {
                // 非远程源码模式回退到常规重启
                stop(id);
                return start(id);
            }
        } catch (Exception e) {
            log.error("轻量重启失败: {}", e.getMessage(), e);
            instanceMapper.updateRuntime(id, "error", null, null, e.getMessage());
            return Map.of("success", false, "message", "重启失败: " + e.getMessage());
        }
    }

    // ==================== 状态/日志/测试 ====================

    public Map<String, Object> getStatus(Long id) {
        OpenClawInstance instance = instanceMapper.findById(id);
        if (instance == null) {
            return Map.of("success", false, "message", "实例不存在");
        }

        Map<String, Object> status = new HashMap<>();
        status.put("id", instance.getId());
        status.put("uid", instance.getUid());
        status.put("status", instance.getStatus());
        status.put("gatewayHost", instance.getGatewayHost());
        status.put("gatewayPort", instance.getGatewayPort());

        if ("running".equals(instance.getStatus())) {
            boolean healthy = checkGatewayHealth(instance);
            status.put("healthy", healthy);
            if (!healthy) {
                instanceMapper.updateStatus(id, "error");
                status.put("status", "error");
            }
        }

        return Map.of("success", true, "data", status);
    }

    public Map<String, Object> getLogs(Long id, Integer lines) {
        OpenClawInstance instance = instanceMapper.findById(id);
        if (instance == null) {
            return Map.of("success", false, "message", "实例不存在");
        }

        int lineCount = lines != null ? lines : 100;

        try {
            boolean isRemote = instance.getWorkstationId() != null;

            if (isRemote) {
                // 远程：通过SSH执行tail命令获取日志
                Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());
                String logPath = instance.getLogsPath() + "/gateway.log";
                String cmd = String.format("tail -n %d %s 2>/dev/null || echo '暂无日志'", lineCount, logPath);
                String logContent = sshRemoteService.executeCommand(creds, cmd);
                return Map.of("success", true, "logs", logContent);
            } else {
                // 本地：直接读取文件
                Path logFile = Paths.get(instance.getLogsPath(), "gateway.log");
                if (!Files.exists(logFile)) {
                    return Map.of("success", true, "logs", "暂无日志");
                }

                List<String> allLines = Files.readAllLines(logFile);
                int start = Math.max(0, allLines.size() - lineCount);
                List<String> logLines = allLines.subList(start, allLines.size());

                return Map.of("success", true, "logs", String.join("\n", logLines));
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", "读取日志失败: " + e.getMessage());
        }
    }

    public Map<String, Object> testConnection(String host, Integer port, String token) {
        try {
            String url = String.format("http://%s:%d/health", host, port);
            WebClient client = webClientBuilder.baseUrl(url).build();

            client.get()
                .headers(h -> {
                    if (token != null && !token.isEmpty()) {
                        h.set("X-OpenClaw-Token", token);
                    }
                })
                .retrieve()
                .toBodilessEntity()
                .block();

            return Map.of("success", true, "message", "连接成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "连接失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法：路径与目录 ====================

    private void setLocalPaths(OpenClawInstance instance) {
        String basePath = System.getProperty("user.home") + "/.openclaw/instances/" + instance.getUid();
        instance.setWorkspacePath(basePath + "/workspace");
        instance.setConfigPath(basePath + "/config");
        instance.setLogsPath(basePath + "/logs");
    }

    private void createLocalDirectories(OpenClawInstance instance) {
        try {
            Files.createDirectories(Paths.get(instance.getWorkspacePath()));
            Files.createDirectories(Paths.get(instance.getConfigPath()));
            Files.createDirectories(Paths.get(instance.getLogsPath()));
        } catch (IOException e) {
            log.error("创建本地目录失败: {}", e.getMessage());
        }
    }

    // ==================== 私有方法：配置生成 ====================

    private Map<String, Object> buildConfigMap(OpenClawInstance instance) throws Exception {
        Map<String, Object> config = new HashMap<>();

        // Gateway配置
        Map<String, Object> gateway = new HashMap<>();
        gateway.put("port", instance.getGatewayPort());
        // bind: "loopback"(仅本机) / "lan"(0.0.0.0，需要auth) / "tailnet"
        gateway.put("bind", "lan");
        // lan 模式必须配置 auth，否则 Gateway 拒绝启动
        Map<String, Object> auth = new HashMap<>();
        if (instance.getGatewayToken() != null && !instance.getGatewayToken().isEmpty()) {
            auth.put("token", instance.getGatewayToken());
        } else {
            // 未设置token时自动生成一个，避免lan模式无auth导致启动失败
            auth.put("token", instance.getUid());
        }
        gateway.put("auth", auth);
        gateway.put("mode", "local");
        // 允许非 HTTPS 环境下的 Control UI 认证（lan 模式远程访问需要）
        Map<String, Object> controlUi = new HashMap<>();
        controlUi.put("allowInsecureAuth", true);
        gateway.put("controlUi", controlUi);
        // 启用 OpenAI 兼容的 HTTP API 端点
        Map<String, Object> chatCompletions = new HashMap<>();
        chatCompletions.put("enabled", true);
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("chatCompletions", chatCompletions);
        Map<String, Object> http = new HashMap<>();
        http.put("endpoints", endpoints);
        gateway.put("http", http);
        config.put("gateway", gateway);

        // 注意：设备自动批准通过 devices/pending.json 文件实现，
        // 而非 openclaw.json 的 devices 配置项（OpenClaw 不识别该顶级键）

        // ==================== 多模型配置 ====================
        // 收集所有配置的模型（主模型 + 回退模型 + 子代理模型）
        String primaryModel = instance.getDefaultModel() != null ? instance.getDefaultModel() : "anthropic/claude-sonnet-4-5";
        List<String> allModels = new ArrayList<>();
        allModels.add(primaryModel);

        // 解析回退模型列表（JSON数组字符串）
        List<String> fallbacks = new ArrayList<>();
        if (instance.getFallbackModels() != null && !instance.getFallbackModels().isEmpty()) {
            try {
                fallbacks = objectMapper.readValue(instance.getFallbackModels(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                allModels.addAll(fallbacks);
            } catch (Exception e) {
                log.warn("解析回退模型列表失败: {}", e.getMessage());
            }
        }

        String subagentModel = instance.getSubagentModel();
        if (subagentModel != null && !subagentModel.isEmpty() && !allModels.contains(subagentModel)) {
            allModels.add(subagentModel);
        }

        // agents.defaults.model — 主模型 + 回退链
        Map<String, Object> model = new HashMap<>();
        model.put("primary", primaryModel);
        if (!fallbacks.isEmpty()) {
            model.put("fallbacks", fallbacks);
        }

        // agents.defaults.models — 白名单（注册所有可用模型）
        Map<String, Object> modelsAllowlist = new HashMap<>();
        for (String m : allModels) {
            modelsAllowlist.put(m, buildModelAlias(m));
        }

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("model", model);
        defaults.put("models", modelsAllowlist);

        // agents.defaults.subagents — 子代理模型（用于子任务的轻量模型）
        if (subagentModel != null && !subagentModel.isEmpty()) {
            Map<String, Object> subagents = new HashMap<>();
            subagents.put("model", subagentModel);
            defaults.put("subagents", subagents);
        }

        Map<String, Object> agents = new HashMap<>();
        agents.put("defaults", defaults);
        config.put("agents", agents);

        // Models.providers 配置：为非内置 provider（如 deepseek）注册 API 端点
        // OpenClaw 内置支持 anthropic/openai/google/groq 等，其他 provider 需要显式配置
        Map<String, Object> providerConfig = buildProviderConfig(allModels);
        if (providerConfig != null && !providerConfig.isEmpty()) {
            Map<String, Object> models = new HashMap<>();
            models.put("mode", "merge");  // 保留内置 provider，合并新增
            models.put("providers", providerConfig);
            config.put("models", models);
        }

        // 渠道配置
        if (instance.getChannelsConfig() != null && !"{}".equals(instance.getChannelsConfig())) {
            Map<String, Object> channels = objectMapper.readValue(instance.getChannelsConfig(), Map.class);
            config.put("channels", channels);
        }

        return config;
    }

    /**
     * 构建模型别名对象（用于 agents.defaults.models 白名单）
     */
    private Map<String, Object> buildModelAlias(String modelName) {
        Map<String, Object> alias = new HashMap<>();
        String provider = getProviderFromModel(modelName);
        String modelId = modelName.contains("/") ? modelName.substring(modelName.indexOf('/') + 1) : modelName;
        String displayName = (provider != null ? provider.substring(0, 1).toUpperCase() + provider.substring(1) : "") + " " + modelId;
        alias.put("alias", displayName);
        return alias;
    }

    private void generateAndDeployConfig(OpenClawInstance instance) throws Exception {
        Map<String, Object> config = buildConfigMap(instance);
        byte[] configBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(config);

        boolean isRemote = instance.getWorkstationId() != null;

        if (isRemote) {
            // 远程：通过SFTP上传配置文件
            Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());
            String remotePath = instance.getConfigPath() + "/openclaw.json";
            sshRemoteService.uploadFile(creds, configBytes, remotePath);
            log.info("配置文件已上传到远程: {}", remotePath);
        } else {
            // 本地：直接写入文件
            Path configFile = Paths.get(instance.getConfigPath(), "openclaw.json");
            Files.createDirectories(configFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), config);
        }
    }

    // ==================== 私有方法：构建API密钥环境变量 ====================

    /**
     * 解析实例的API密钥明文：优先使用托管密钥引用，回退到直接存储的加密密钥
     */
    private String resolveApiKeyPlaintext(OpenClawInstance instance) {
        // 优先从API密钥管理模块获取（通过引用ID）
        if (instance.getApiKeyRefId() != null) {
            String key = apiKeyService.decryptApiKeyById(instance.getApiKeyRefId());
            if (key != null) return key;
        }
        // 回退：使用直接存储的AES加密密钥
        if (instance.getApiKeyId() != null) {
            return decryptApiKey(instance.getApiKeyId());
        }
        return null;
    }

    private String buildApiKeyEnvExport(OpenClawInstance instance) {
        String provider = getProviderFromModel(instance.getDefaultModel());
        if (provider != null) {
            String decryptedKey = resolveApiKeyPlaintext(instance);
            if (decryptedKey != null) {
                String envKey = provider.toUpperCase() + "_API_KEY";
                log.info("已设置 LLM provider 环境变量: {}={}...", envKey,
                        decryptedKey.substring(0, Math.min(8, decryptedKey.length())));
                return String.format("export %s='%s'", envKey, decryptedKey.replace("'", "'\\''"));
            } else {
                log.warn("无法解密API密钥！实例 {} apiKeyRefId={} apiKeyId={}。"
                                + "旧密钥可能缺少 encryptedKey 字段，请到API密钥管理中重新生成或更新该密钥",
                        instance.getUid(), instance.getApiKeyRefId(), instance.getApiKeyId());
            }
        } else {
            log.warn("无法从模型名 '{}' 推断 LLM provider，未设置 API key 环境变量", instance.getDefaultModel());
        }
        return "";
    }

    private void addApiKeyEnvVar(OpenClawInstance instance, List<String> command) {
        String provider = getProviderFromModel(instance.getDefaultModel());
        if (provider != null) {
            String decryptedKey = resolveApiKeyPlaintext(instance);
            if (decryptedKey != null) {
                String envKey = provider.toUpperCase() + "_API_KEY";
                command.add("-e");
                command.add(envKey + "=" + decryptedKey);
            }
        }
    }

    private void addApiKeyEnvVarToMap(OpenClawInstance instance, Map<String, String> env) {
        String provider = getProviderFromModel(instance.getDefaultModel());
        if (provider != null) {
            String decryptedKey = resolveApiKeyPlaintext(instance);
            if (decryptedKey != null) {
                String envKey = provider.toUpperCase() + "_API_KEY";
                env.put(envKey, decryptedKey);
            }
        }
    }

    private String getProviderFromModel(String model) {
        if (model == null) return null;
        if (model.startsWith("anthropic/")) return "anthropic";
        if (model.startsWith("openai/")) return "openai";
        if (model.startsWith("google/")) return "google";
        if (model.startsWith("deepseek/")) return "deepseek";
        if (model.startsWith("together/")) return "together";
        if (model.startsWith("groq/")) return "groq";
        return null;
    }

    /**
     * 为非内置 provider 构建 models.providers 配置。
     * OpenClaw 内置提供商（anthropic, openai, google, groq, openrouter, xai, cerebras,
     * mistral, github-copilot, ollama 等）不需要此配置，只需设置环境变量。
     * 非内置提供商（如 deepseek, together）需要通过 models.providers 显式注册。
     * apiKey 通过 ${ENV_VAR} 引用环境变量（由脚本 export 注入）。
     *
     * @param modelNames 所有需要注册的模型列表（主模型+回退+子代理）
     */
    private Map<String, Object> buildProviderConfig(List<String> modelNames) {
        if (modelNames == null || modelNames.isEmpty()) return null;

        // 按 provider 分组收集模型，跳过内置 provider
        // key: provider名称, value: 该provider下的modelId列表
        Map<String, List<String>> providerModels = new LinkedHashMap<>();

        for (String modelName : modelNames) {
            String provider = getProviderFromModel(modelName);
            if (provider == null) continue;
            // 内置 provider 无需 models.providers 配置
            if (isBuiltinProvider(provider)) continue;

            String modelId = modelName.contains("/") ? modelName.substring(modelName.indexOf('/') + 1) : modelName;
            providerModels.computeIfAbsent(provider, k -> new ArrayList<>()).add(modelId);
        }

        if (providerModels.isEmpty()) return null;

        Map<String, Object> providers = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : providerModels.entrySet()) {
            String provider = entry.getKey();
            List<String> modelIds = entry.getValue();

            String baseUrl = getProviderBaseUrl(provider);
            String api = getProviderApi(provider);
            String envVar = provider.toUpperCase() + "_API_KEY";

            if (baseUrl == null) continue;

            Map<String, Object> providerEntry = new HashMap<>();
            providerEntry.put("baseUrl", baseUrl);
            providerEntry.put("apiKey", "${" + envVar + "}");
            providerEntry.put("api", api);

            // 注册该 provider 下的所有模型
            List<Map<String, String>> modelDefs = new ArrayList<>();
            for (String modelId : modelIds) {
                String displayName = provider.substring(0, 1).toUpperCase() + provider.substring(1) + " " + modelId;
                modelDefs.add(Map.of("id", modelId, "name", displayName));
            }
            providerEntry.put("models", modelDefs);

            providers.put(provider, providerEntry);
        }

        return providers.isEmpty() ? null : providers;
    }

    /**
     * 判断 provider 是否为 OpenClaw 内置支持（无需 models.providers 配置）
     */
    private boolean isBuiltinProvider(String provider) {
        return Set.of("anthropic", "openai", "google", "groq", "openrouter",
                "xai", "cerebras", "mistral", "ollama").contains(provider);
    }

    /**
     * 获取非内置 provider 的 API 基础地址
     */
    private String getProviderBaseUrl(String provider) {
        switch (provider) {
            case "deepseek": return "https://api.deepseek.com/v1";
            case "together": return "https://api.together.xyz/v1";
            default: return null;
        }
    }

    /**
     * 获取非内置 provider 的 API 协议类型
     */
    private String getProviderApi(String provider) {
        switch (provider) {
            case "deepseek":
            case "together":
                return "openai-completions";
            default:
                return "openai-completions";
        }
    }

    // ==================== 私有方法：启动（远程+本地） ====================

    private Map<String, Object> startWithDocker(OpenClawInstance instance) throws Exception {
        String containerName = "openclaw-" + instance.getUid().toLowerCase();
        boolean isRemote = instance.getWorkstationId() != null;

        if (isRemote) {
            Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());

            // 构建远程Docker命令
            StringBuilder cmd = new StringBuilder();
            cmd.append("docker run -d");
            cmd.append(" --name ").append(containerName);
            cmd.append(" -p ").append(instance.getGatewayPort()).append(":18789");
            cmd.append(" -v ").append(instance.getConfigPath()).append(":/home/node/.openclaw");
            cmd.append(" -v ").append(instance.getWorkspacePath()).append(":/home/node/.openclaw/workspace");
            cmd.append(" -e HOME=/home/node");

            // API密钥环境变量
            String provider = getProviderFromModel(instance.getDefaultModel());
            if (provider != null) {
                String decryptedKey = resolveApiKeyPlaintext(instance);
                if (decryptedKey != null) {
                    cmd.append(" -e ").append(provider.toUpperCase()).append("_API_KEY='")
                       .append(decryptedKey.replace("'", "'\\''")).append("'");
                }
            }

            cmd.append(" openclaw:latest gateway --bind lan");

            String output = sshRemoteService.executeCommand(creds, cmd.toString());
            log.info("远程Docker启动输出: {}", output);

            instanceMapper.updateRuntime(instance.getId(), "running", containerName, null, null);
            return Map.of("success", true, "message", "远程Docker容器启动成功", "containerName", containerName);
        } else {
            // 本地Docker启动（保持原有逻辑）
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("run");
            command.add("-d");
            command.add("--name");
            command.add(containerName);
            command.add("-p");
            command.add(instance.getGatewayPort() + ":18789");
            command.add("-v");
            command.add(instance.getConfigPath() + ":/home/node/.openclaw");
            command.add("-v");
            command.add(instance.getWorkspacePath() + ":/home/node/.openclaw/workspace");
            command.add("-e");
            command.add("HOME=/home/node");

            addApiKeyEnvVar(instance, command);

            command.add("openclaw:latest");
            command.add("gateway");
            command.add("--bind");
            command.add("lan");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = new String(process.getInputStream().readAllBytes());
                throw new RuntimeException("Docker启动失败: " + error);
            }

            instanceMapper.updateRuntime(instance.getId(), "running", containerName, null, null);
            return Map.of("success", true, "message", "Docker容器启动成功", "containerName", containerName);
        }
    }

    private Map<String, Object> startWithNpm(OpenClawInstance instance) throws Exception {
        boolean isRemote = instance.getWorkstationId() != null;

        if (isRemote) {
            Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());

            String apiKeyExport = buildApiKeyEnvExport(instance);
            String cmd = String.format(
                    "export OPENCLAW_STATE_DIR='%s' && %s && " +
                    "nohup npx openclaw gateway --port %d --verbose > %s/gateway.log 2>&1 & echo $!",
                    instance.getConfigPath(),
                    apiKeyExport.isEmpty() ? "true" : apiKeyExport,
                    instance.getGatewayPort(),
                    instance.getLogsPath()
            );

            String pid = sshRemoteService.executeCommand(creds, cmd).trim();
            log.info("远程NPM进程启动, PID: {}", pid);

            instanceMapper.updateRuntime(instance.getId(), "running", null, pid, null);
            return Map.of("success", true, "message", "远程NPM进程启动成功", "pid", pid);
        } else {
            // 本地NPM启动（保持原有逻辑）
            List<String> command = new ArrayList<>();
            command.add("npx");
            command.add("openclaw");
            command.add("gateway");
            command.add("--port");
            command.add(String.valueOf(instance.getGatewayPort()));
            command.add("--verbose");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(instance.getWorkspacePath()));
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            env.put("OPENCLAW_STATE_DIR", instance.getConfigPath());
            addApiKeyEnvVarToMap(instance, env);

            Path logFile = Paths.get(instance.getLogsPath(), "gateway.log");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            Process process = pb.start();
            runningProcesses.put(instance.getId(), process);

            String pid = String.valueOf(process.pid());
            instanceMapper.updateRuntime(instance.getId(), "running", null, pid, null);

            return Map.of("success", true, "message", "NPM进程启动成功", "pid", pid);
        }
    }

    private Map<String, Object> startFromSource(OpenClawInstance instance) throws Exception {
        boolean isRemote = instance.getWorkstationId() != null;

        if (isRemote) {
            return deployAndStartRemoteSource(instance);
        } else {
            // 本地源码启动（保持原有逻辑）
            String sourcePath = System.getProperty("user.home") + "/openclaw-source";

            List<String> command = new ArrayList<>();
            command.add("pnpm");
            command.add("openclaw");
            command.add("gateway");
            command.add("--port");
            command.add(String.valueOf(instance.getGatewayPort()));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(sourcePath));
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            env.put("OPENCLAW_STATE_DIR", instance.getConfigPath());
            addApiKeyEnvVarToMap(instance, env);

            Path logFile = Paths.get(instance.getLogsPath(), "gateway.log");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            Process process = pb.start();
            runningProcesses.put(instance.getId(), process);

            String pid = String.valueOf(process.pid());
            instanceMapper.updateRuntime(instance.getId(), "running", null, pid, null);

            return Map.of("success", true, "message", "源码进程启动成功", "pid", pid);
        }
    }

    // ==================== 私有方法：停止/健康检查 ====================

    private void stopLocalDockerContainer(String containerName) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("docker", "stop", containerName);
        Process process = pb.start();
        process.waitFor(30, TimeUnit.SECONDS);

        pb = new ProcessBuilder("docker", "rm", containerName);
        process = pb.start();
        process.waitFor(10, TimeUnit.SECONDS);
    }

    private boolean checkGatewayHealth(OpenClawInstance instance) {
        try {
            String host = instance.getGatewayHost() != null ? instance.getGatewayHost() : "localhost";
            String url = String.format("http://%s:%d/health", host, instance.getGatewayPort());

            WebClient client = webClientBuilder.baseUrl(url).build();
            client.get().retrieve().toBodilessEntity().block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 私有方法：远程源码部署 ====================

    /**
     * 加载部署脚本模板并替换变量
     */
    private String loadDeployScript(OpenClawInstance instance) throws IOException {
        ClassPathResource resource = new ClassPathResource("scripts/openclaw-deploy-linux.sh");
        byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String template = new String(bytes, StandardCharsets.UTF_8);

        // 替换变量
        template = template.replace("${GATEWAY_PORT}", String.valueOf(instance.getGatewayPort()));
        template = template.replace("${CONFIG_PATH}", instance.getConfigPath());
        template = template.replace("${LOGS_PATH}", instance.getLogsPath());

        // 源码路径：与 config/logs 同级的 source 目录
        String basePath = instance.getConfigPath().replace("/config", "");
        template = template.replace("${SOURCE_PATH}", basePath + "/source");

        // API密钥环境变量
        String apiKeyExport = buildApiKeyEnvExport(instance);
        log.info("轻量重启脚本 API_KEY_ENV: {}", apiKeyExport.isEmpty() ? "(empty)" :
                apiKeyExport.substring(0, Math.min(40, apiKeyExport.length())) + "...");
        template = template.replace("${API_KEY_ENV}", apiKeyExport.isEmpty() ? "# no API key configured" : apiKeyExport);

        // 统一换行符为Unix格式
        template = template.replace("\r\n", "\n").replace("\r", "\n");

        return template;
    }

    /**
     * 加载轻量重启脚本模板并替换变量
     */
    private String loadRestartScript(OpenClawInstance instance) throws IOException {
        ClassPathResource resource = new ClassPathResource("scripts/openclaw-restart-linux.sh");
        byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String template = new String(bytes, StandardCharsets.UTF_8);

        // 替换变量
        template = template.replace("${GATEWAY_PORT}", String.valueOf(instance.getGatewayPort()));
        template = template.replace("${CONFIG_PATH}", instance.getConfigPath());
        template = template.replace("${LOGS_PATH}", instance.getLogsPath());

        String basePath = instance.getConfigPath().replace("/config", "");
        template = template.replace("${SOURCE_PATH}", basePath + "/source");

        String apiKeyExport = buildApiKeyEnvExport(instance);
        template = template.replace("${API_KEY_ENV}", apiKeyExport.isEmpty() ? "# no API key configured" : apiKeyExport);

        // 统一换行符为Unix格式
        template = template.replace("\r\n", "\n").replace("\r", "\n");

        return template;
    }

    /**
     * 远程源码模式轻量重启：仅停止进程+配置+启动（跳过环境安装和构建）
     */
    private Map<String, Object> restartRemoteSourceQuick(OpenClawInstance instance) throws Exception {
        log.info("开始轻量重启 OpenClaw 实例 {} 到工作站 {}", instance.getUid(), instance.getWorkstationId());

        Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());

        // 1. 加载轻量重启脚本
        String script = loadRestartScript(instance);

        // 2. 上传脚本
        String scriptPath = "/tmp/openclaw_restart_" + System.currentTimeMillis() + ".sh";
        sshRemoteService.uploadFile(creds, script.getBytes(StandardCharsets.UTF_8), scriptPath);
        log.info("轻量重启脚本已上传: {}", scriptPath);

        // 3. 执行脚本
        log.info("执行轻量重启脚本...");
        String output = sshRemoteService.executeCommand(creds, "chmod +x " + scriptPath + " && bash " + scriptPath, 0);
        log.info("轻量重启脚本执行完成，输出:\n{}", output);

        // 4. 提取PID
        String pid = null;
        for (String line : output.split("\n")) {
            if (line.startsWith("OPENCLAW_PID=")) {
                pid = line.replace("OPENCLAW_PID=", "").trim();
                break;
            }
        }

        // 5. 判断结果
        if (output.contains("重启成功") && pid != null) {
            instanceMapper.updateRuntime(instance.getId(), "running", null, pid, null);
            log.info("轻量重启成功, PID: {}", pid);
            return Map.of("success", true, "message", "重启成功（轻量模式）", "pid", pid);
        } else if (output.contains("重启成功") || pid != null) {
            instanceMapper.updateRuntime(instance.getId(), "running", null, pid, null);
            log.info("轻量重启部分成功, PID: {}", pid);
            return Map.of("success", true, "message", "重启完成，Gateway可能仍在初始化", "pid", pid != null ? pid : "unknown");
        } else if (output.contains("[ERROR]") || output.contains("exit 1")) {
            String errorMsg = "轻量重启失败，请查看日志";
            instanceMapper.updateRuntime(instance.getId(), "error", null, null, errorMsg);
            return Map.of("success", false, "message", errorMsg);
        } else {
            String errorMsg = "重启脚本执行异常：未检测到Gateway启动信息，请查看日志";
            log.warn("轻量重启输出不完整。输出末尾: {}",
                    output.length() > 500 ? output.substring(output.length() - 500) : output);
            instanceMapper.updateRuntime(instance.getId(), "error", null, null, errorMsg);
            return Map.of("success", false, "message", errorMsg);
        }
    }

    /**
     * 远程部署并启动OpenClaw（源码模式）
     * 完整流程：安装Node.js → pnpm → 克隆源码 → 构建 → 启动Gateway
     */
    private Map<String, Object> deployAndStartRemoteSource(OpenClawInstance instance) throws Exception {
        log.info("开始远程部署 OpenClaw 到工作站 {}", instance.getWorkstationId());

        Map<String, Object> creds = sshRemoteService.fetchCredentials(instance.getWorkstationId());

        // 1. 加载并处理部署脚本
        String script = loadDeployScript(instance);

        // 2. 上传脚本到远程服务器
        String scriptPath = "/tmp/openclaw_deploy_" + System.currentTimeMillis() + ".sh";
        sshRemoteService.uploadFile(creds, script.getBytes(StandardCharsets.UTF_8), scriptPath);
        log.info("部署脚本已上传: {}", scriptPath);

        // 3. 执行部署脚本（可能耗时较长：安装依赖+构建）
        log.info("开始执行远程部署脚本，这可能需要几分钟...");
        String output = sshRemoteService.executeCommand(creds, "chmod +x " + scriptPath + " && bash " + scriptPath, 0);
        log.info("远程部署脚本执行完成，输出:\n{}", output);

        // 4. 从输出中提取PID
        String pid = null;
        for (String line : output.split("\n")) {
            if (line.startsWith("OPENCLAW_PID=")) {
                pid = line.replace("OPENCLAW_PID=", "").trim();
                break;
            }
        }

        // 5. 判断部署结果
        if (output.contains("OpenClaw Gateway 启动成功") && pid != null) {
            instanceMapper.updateRuntime(instance.getId(), "running", null, pid, null);
            log.info("远程部署成功, PID: {}", pid);
            return Map.of("success", true, "message", "远程部署并启动成功", "pid", pid);
        } else if (output.contains("OpenClaw Gateway 启动成功") || pid != null) {
            // 有PID但没有"启动成功"，或有"启动成功"但没PID — 部分成功
            instanceMapper.updateRuntime(instance.getId(), "running", null, pid, null);
            log.info("远程部署部分成功, PID: {}", pid);
            return Map.of("success", true, "message", "部署完成，Gateway可能仍在初始化", "pid", pid != null ? pid : "unknown");
        } else if (output.contains("[ERROR]") || output.contains("exit 1")) {
            String errorMsg = "远程部署失败，请查看日志";
            instanceMapper.updateRuntime(instance.getId(), "error", null, null, errorMsg);
            return Map.of("success", false, "message", errorMsg);
        } else {
            // 脚本输出不完整（未见启动成功或PID），标记为error而非running
            String errorMsg = "部署脚本执行异常：未检测到Gateway启动信息，请查看日志";
            log.warn("远程部署输出不完整，未检测到启动信息。输出末尾: {}",
                    output.length() > 500 ? output.substring(output.length() - 500) : output);
            instanceMapper.updateRuntime(instance.getId(), "error", null, null, errorMsg);
            return Map.of("success", false, "message", errorMsg);
        }
    }
}
