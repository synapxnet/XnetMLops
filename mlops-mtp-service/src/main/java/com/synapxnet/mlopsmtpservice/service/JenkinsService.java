package com.synapxnet.mlopsmtpservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsmtpservice.entity.JenkinsBuildStatus;
import com.synapxnet.mlopsmtpservice.entity.PipelineConfigParams;
import com.synapxnet.mlopsmtpservice.entity.TaskInfo;
import com.synapxnet.mlopsmtpservice.exception.JobNotFoundException;
import com.synapxnet.mlopsmtpservice.service.ScheduleService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JenkinsService {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsService.class);

    @Autowired
    @Qualifier("jenkinsBuildStatusRedisTemplate")
    private RedisTemplate<String, JenkinsBuildStatus> redisTemplate;

    @Autowired
    private TaskInfoService taskInfoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String username;

    @Value("${jenkins.api-token}")
    private String apiToken;

    @Autowired
    private ScheduleService scheduleService;

    // 获取 CSRF Crumb
    private String getCrumb() throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            URI uri = new URIBuilder(jenkinsUrl + "/crumbIssuer/api/json").build();
            HttpGet request = new HttpGet(uri);
            request.addHeader("Authorization", getAuthHeader());

            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Failed to get crumb: " + response.getStatusLine());
            }

            String json = EntityUtils.toString(response.getEntity());
            if (json.contains("\"crumb\":")) {
                return json.split("\"crumb\":\"")[1].split("\"")[0];
            }
            return null;
        }
    }

    // 创建认证头
    private String getAuthHeader() {
        String auth = username + ":" + apiToken;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    /**
     * 规范化 Jenkins URL，将 Jenkins 返回的 URL 替换为配置的 Jenkins URL
     * Jenkins 内部可能配置了不同的访问地址，需要统一使用后端配置的地址
     */
    private String normalizeJenkinsUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        try {
            // 解析 Jenkins 返回的 URL
            URI originalUri = new URI(url);
            // 解析配置的 Jenkins URL
            URI configuredUri = new URI(jenkinsUrl);

            // 用配置的 host 和 port 替换原始 URL 中的
            URI normalizedUri = new URI(
                configuredUri.getScheme(),
                null,
                configuredUri.getHost(),
                configuredUri.getPort(),
                originalUri.getPath(),
                originalUri.getQuery(),
                originalUri.getFragment()
            );
            return normalizedUri.toString();
        } catch (URISyntaxException e) {
            logger.warn("URL 规范化失败: {}, 使用原始 URL", e.getMessage());
            return url;
        }
    }

    // 构建作业URL（支持多级文件夹）
    private String buildJobUrl(String jobPath) throws URISyntaxException {
        String baseUrl = jenkinsUrl.endsWith("/") ? jenkinsUrl : jenkinsUrl + "/";
        String[] pathSegments = jobPath.split("/");
        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        for (String segment : pathSegments) {
            urlBuilder.append("job/")
                    .append(URLEncoder.encode(segment, StandardCharsets.UTF_8))
                    .append("/");
        }

        return urlBuilder.substring(0, urlBuilder.length() - 1);
    }

    // 创建作业方法（支持文件夹路径）
    public String createJob(String jobPath, String jobConfig) throws IOException, URISyntaxException {
        int lastSlashIndex = jobPath.lastIndexOf('/');
        String jobName = (lastSlashIndex != -1) ? jobPath.substring(lastSlashIndex + 1) : jobPath;
        String folderPath = (lastSlashIndex != -1) ? jobPath.substring(0, lastSlashIndex) : "";

        String createUrl;
        if (!folderPath.isEmpty()) {
            createUrl = buildJobUrl(folderPath) + "/createItem";
        } else {
            createUrl = jenkinsUrl + "/createItem";
        }

        URI uri = new URIBuilder(createUrl)
                .addParameter("name", URLEncoder.encode(jobName, StandardCharsets.UTF_8))
                .build();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(uri);
            post.addHeader("Content-Type", "application/xml;charset=UTF-8");
            post.setEntity(new StringEntity(jobConfig, StandardCharsets.UTF_8));
            post.addHeader("Authorization", getAuthHeader());

            String crumb = getCrumb();
            if (crumb != null) {
                post.addHeader("Jenkins-Crumb", crumb);
            }

            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                return "Job created successfully";
            } else {
                String errorBody = EntityUtils.toString(response.getEntity());
                throw new IOException(
                        "Failed to create job. Status: " + statusCode +
                                ", Response: " + errorBody
                );
            }
        }
    }

    // 动态创建作业方法
    public String createDynamicJob(String jobName, PipelineConfigParams params) throws IOException, URISyntaxException {
        Map<String, String> templateParams = new HashMap<>();

        templateParams.put("description", params.getDescription());
        templateParams.put("defaultBranch", params.getDefaultBranch());
        templateParams.put("defaultEnvironment", params.getDefaultEnvironment());
        templateParams.put("datasetPath", params.getDatasetPath());
        templateParams.put("datasetFolder", params.getDatasetFolder());
        templateParams.put("gitRepoUrl", params.getGitRepoUrl());
        templateParams.put("credentialsId", params.getCredentialsId());
        templateParams.put("GIT_BRANCH", params.getGitBranch());
        templateParams.put("pythonEntryPoint", params.getPythonEntryPoint());
        templateParams.put("containerName", params.getContainerName());
        templateParams.put("modelOutputPath", params.getModelOutputPath());
        templateParams.put("containerToRemove", params.getContainerToRemove());
        templateParams.put("algorithmName", params.getAlgorithmName());
        templateParams.put("algorithmVersion", params.getAlgorithmVersion());
        templateParams.put("dockerImageName", params.getDockerImageName());
        if (params.getDockerImageTags() != null) {
            String tags = params.getDockerImageTags().replace("-", ",");
            templateParams.put("dockerImageTags", tags);
        } else {
            templateParams.put("dockerImageTags", "");
        }

        if (params.getScheduleConfig() != null && !params.getScheduleConfig().isEmpty()) {
            // 解析调度配置
            boolean scheduleEnabled = scheduleService.isScheduleActive(params.getScheduleConfig());
            String cronExpression = scheduleService.convertToCronExpression(params.getScheduleConfig());

            templateParams.put("SCHEDULE_ENABLED", String.valueOf(scheduleEnabled));
            templateParams.put("CRON_EXPRESSION", cronExpression);

            logger.info("调度配置解析结果 - enabled: {}, cron: {}", scheduleEnabled, cronExpression);
        } else {
            // 默认不启用调度
            templateParams.put("SCHEDULE_ENABLED", "false");
            templateParams.put("CRON_EXPRESSION", "");
        }

        templateParams.put("harborUrl", params.getHarborUrl());
        templateParams.put("harborCredentialsId", params.getHarborCredentialsId());

        templateParams.putIfAbsent("containerName", params.getContainerName());
        templateParams.putIfAbsent("containerToRemove", params.getContainerName());
        templateParams.putIfAbsent("datasetFolder", "dataset");

        String jobConfig = generatePipelineConfig(templateParams);
        return createJob(jobName, jobConfig);
    }

    // 模板处理逻辑
    private String generatePipelineConfig(Map<String, String> parameters) throws IOException {
        ClassPathResource resource = new ClassPathResource("jenkins-templates/pipeline-template.xml");
        String template;
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            template = new String(bytes, StandardCharsets.UTF_8);
        }

        logger.info("Pipeline template parameters: {}", parameters);
        return replacePlaceholders(template, parameters);
    }

    // 占位符替换逻辑
    private String replacePlaceholders(String template, Map<String, String> parameters) {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            template = template.replace(placeholder,
                    escapeXml(entry.getValue() != null ? entry.getValue() : ""));
        }

        String cdataStart = "<![CDATA[";
        String cdataEnd = "]]>";
        int start = template.indexOf(cdataStart);
        int end = template.indexOf(cdataEnd);

        if (start != -1 && end != -1) {
            String cdataContent = template.substring(start + cdataStart.length(), end);
            for (String param : parameters.keySet()) {
                String placeholder = "${" + param + "}";
                String value = parameters.get(param) != null ? parameters.get(param) : "";
                cdataContent = cdataContent.replace(placeholder, value);
            }

            template = template.substring(0, start) +
                    cdataStart + cdataContent +
                    cdataEnd + template.substring(end + cdataEnd.length());
        }

        return template;
    }

    // XML 特殊字符转义
    private String escapeXml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // 检查作业是否存在
    public boolean jobExists(String jobPath) throws IOException, URISyntaxException {
        String jobUrl = buildJobUrl(jobPath) + "/api/json";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpHead head = new HttpHead(jobUrl);
            head.addHeader("Authorization", getAuthHeader());

            HttpResponse response = client.execute(head);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        }
    }

    // 触发构建（支持多级文件夹路径）
    public String triggerBuild(String jobPath, String parentTaskUID)
            throws IOException, URISyntaxException, JobNotFoundException {
        if (!jobExists(jobPath)) {
            throw new JobNotFoundException("Job not found: " + jobPath);
        }

        String buildUrl = buildJobUrl(jobPath) + "/buildWithParameters";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(buildUrl);
            post.addHeader("Authorization", getAuthHeader());
            post.setEntity(new StringEntity("<run/>", ContentType.APPLICATION_XML));

            String crumb = getCrumb();
            if (crumb != null) {
                post.addHeader("Jenkins-Crumb", crumb);
            }

            post.addHeader("Accept", "application/json");

            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_CREATED) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new IOException("Trigger failed. Status: " + statusCode +
                        ", Response: " + responseBody);
            }

            String queueLocation = response.getFirstHeader("Location").getValue();

            JenkinsBuildStatus status = new JenkinsBuildStatus();
            status.setJobName(jobPath);
            status.setParentTaskUID(parentTaskUID);
            status.setQueueUrl(queueLocation);
            status.setOverallStatus("QUEUED");
            status.setStartTime(new Date());

            saveBuildStatus(jobPath, status);

            new Thread(() -> trackBuildStatus(jobPath, queueLocation)).start();

            return queueLocation;
        }
    }

    // 跟踪构建状态 - 修复状态跟踪问题
    private void trackBuildStatus(String jobPath, String queueUrl) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String buildUrl = waitForBuildStart(client, queueUrl);
            if (buildUrl == null) {
                logger.error("Build did not start within timeout for job: {}", jobPath);
                updateStatus(jobPath, "TIMEOUT", "Build did not start within timeout");
                return;
            }

            JenkinsBuildStatus status = getBuildStatus(jobPath);
            if (status == null) {
                logger.error("无法从Redis获取构建状态，跳过跟踪: {}", jobPath);
                return;
            }
            status.setBuildUrl(normalizeJenkinsUrl(buildUrl));
            status.setOverallStatus("IN_PROGRESS");
            saveBuildStatus(jobPath, status);

            // 关键修复：添加构建完成标志和重试机制
            boolean buildCompleted = false;
            int consecutiveFailures = 0;
            final int MAX_CONSECUTIVE_FAILURES = 5;
            final long TIMEOUT = 30 * 60 * 1000; // 30分钟超时
            final long startTime = System.currentTimeMillis();

            while (!buildCompleted && (System.currentTimeMillis() - startTime < TIMEOUT)) {
                Thread.sleep(5000);

                JenkinsBuildStatus currentStatus = getBuildStatus(jobPath);
                if (currentStatus == null) {
                    logger.warn("Build status disappeared for job: {}", jobPath);
                    return;
                }

                try {
                    JSONObject buildInfo = getBuildInfo(client, buildUrl);
                    if (buildInfo == null) {
                        consecutiveFailures++;
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            logger.error("Max consecutive failures reached for job: {}", jobPath);
                            updateStatus(jobPath, "ERROR", "Failed to get build info");
                            return;
                        }
                        continue;
                    }

                    consecutiveFailures = 0; // 重置失败计数器

                    List<JenkinsBuildStatus.StageInfo> stages = parseStages(buildInfo);
                    currentStatus.setStages(stages);

                    // 修复：总是更新开始时间
                    if (buildInfo.has("timestamp")) {
                        long timestamp = buildInfo.getLong("timestamp");
                        currentStatus.setStartTime(new Date(timestamp));
                    }

                    // 关键修复：更可靠的状态检测
                    if (buildInfo.has("result") && !buildInfo.isNull("result")) {
                        String result = buildInfo.getString("result");
                        currentStatus.setOverallStatus(result);

                        try {
                            currentStatus.setConsoleOutput(getConsoleOutput(client, buildUrl));
                        } catch (Exception e) {
                            logger.error("Failed to get console output: {}", e.getMessage());
                        }

                        // 设置结束时间
                        if (buildInfo.has("duration")) {
                            long duration = buildInfo.getLong("duration");
                            currentStatus.setEndTime(new Date(currentStatus.getStartTime().getTime() + duration));
                        }

                        saveBuildStatus(jobPath, currentStatus);
                        logger.info("Build completed for job: {} with status: {}", jobPath, result);
                        buildCompleted = true;
                    } else {
                        // 检查构建是否实际完成 - 通过控制台输出
                        String consoleText = getConsoleOutput(client, buildUrl + "/consoleText");

                        // 检测Jenkins构建结束标志
                        if (consoleText.contains("Finished: SUCCESS") ||
                                consoleText.contains("Finished: FAILURE") ||
                                consoleText.contains("Finished: ABORTED")) {

                            // 手动设置完成状态
                            String result = "UNKNOWN";
                            if (consoleText.contains("Finished: SUCCESS")) result = "SUCCESS";
                            if (consoleText.contains("Finished: FAILURE")) result = "FAILURE";
                            if (consoleText.contains("Finished: ABORTED")) result = "ABORTED";

                            currentStatus.setOverallStatus(result);
                            currentStatus.setConsoleOutput(consoleText);
                            currentStatus.setEndTime(new Date());

                            saveBuildStatus(jobPath, currentStatus);
                            logger.warn("Manually detected build completion: {}", result);
                            buildCompleted = true;
                        }
                    }

                    saveBuildStatus(jobPath, currentStatus);
                } catch (Exception e) {
                    consecutiveFailures++;
                    logger.error("Error while tracking build: {}", e.getMessage());
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        logger.error("Max consecutive failures reached for job: {}", jobPath);
                        updateStatus(jobPath, "ERROR", "Tracking failed: " + e.getMessage());
                        return;
                    }
                }
            }

            // 最终确认：确保状态被保存
            if (!buildCompleted) {
                logger.error("Build status tracking timed out for job: {}", jobPath);
                updateStatus(jobPath, "TIMEOUT", "Build did not complete within timeout");
            }
        } catch (Exception e) {
            logger.error("Error tracking build status for job: " + jobPath, e);
            updateStatus(jobPath, "ERROR", "Error tracking build: " + e.getMessage());
        }
    }

    // 保存或更新任务信息到数据库
    private void saveOrUpdateTaskInfo(String jobPath, JenkinsBuildStatus status) {
        try {
            // 检查必要字段
            if (status.getParentTaskUID() == null || status.getParentTaskUID().isEmpty()) {
                logger.warn("保存任务信息时 parentTaskUID 为空, jobPath={}", jobPath);
                return;
            }

            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setJob_uid(jobPath);
            taskInfo.setTask_uid(status.getParentTaskUID());
            taskInfo.setJob_status(status.getOverallStatus());
            taskInfo.setStart_at(status.getStartTime());
            taskInfo.setEnd_at(status.getEndTime());

            taskInfo.setJob_content(objectMapper.writeValueAsString(status));
            taskInfo.setSchedule_active(0);

            logger.info("准备保存任务信息: jobPath={}, taskUid={}, status={}",
                    jobPath, status.getParentTaskUID(), status.getOverallStatus());
            taskInfoService.saveOrUpdateTaskInfo(taskInfo);
            logger.info("任务信息已保存或更新: jobPath={}, status={}", jobPath, status.getOverallStatus());
        } catch (JsonProcessingException e) {
            logger.error("序列化构建状态失败", e);
        } catch (Exception e) {
            logger.error("保存任务信息到数据库失败: {}", e.getMessage(), e);
        }
    }

    // 保存构建状态 - 确保最终状态被保存
    private void saveBuildStatus(String jobName, JenkinsBuildStatus status) {
        try {
            logger.info("saveBuildStatus 被调用: jobName={}, parentTaskUID={}, status={}",
                    jobName, status.getParentTaskUID(), status.getOverallStatus());

            // 保存到Redis
            redisTemplate.opsForValue().set(jobName, status, 24, TimeUnit.HOURS);
            logger.info("已保存到Redis: jobName={}", jobName);

            // 同步更新到数据库
            saveOrUpdateTaskInfo(jobName, status);
        } catch (Exception e) {
            logger.error("保存构建状态失败: {}", jobName, e);
        }
    }

    private void updateStatus(String jobName, String status, String message) {
        JenkinsBuildStatus buildStatus = getBuildStatus(jobName);
        if (buildStatus == null) {
            buildStatus = new JenkinsBuildStatus();
            buildStatus.setJobName(jobName);
        }
        buildStatus.setOverallStatus(status);
        buildStatus.setConsoleOutput(message);
        saveBuildStatus(jobName, buildStatus);
    }

    // ================= 辅助方法 =================
    private String waitForBuildStart(CloseableHttpClient client, String queueUrl)
            throws IOException, InterruptedException {
        int attempts = 0;
        while (attempts < 30) {
            attempts++;

            try {
                HttpGet request = new HttpGet(queueUrl + "/api/json");
                request.addHeader("Authorization", getAuthHeader());

                HttpResponse response = client.execute(request);
                String json = EntityUtils.toString(response.getEntity());
                JSONObject queueInfo = new JSONObject(json);

                if (queueInfo.has("executable") && !queueInfo.isNull("executable")) {
                    JSONObject executable = queueInfo.getJSONObject("executable");
                    return executable.getString("url");
                }

                if (queueInfo.has("cancelled") && queueInfo.getBoolean("cancelled")) {
                    return null;
                }
            } catch (Exception e) {
                logger.warn("检查队列状态错误: " + e.getMessage());
            }

            Thread.sleep(5000);
        }
        return null;
    }

    private JSONObject getBuildInfo(CloseableHttpClient client, String buildUrl) throws IOException {
        if (buildUrl.endsWith("/")) {
            buildUrl = buildUrl.substring(0, buildUrl.length() - 1);
        }

        String apiUrl = buildUrl + "/wfapi/describe";
        HttpGet request = new HttpGet(apiUrl);
        request.addHeader("Authorization", getAuthHeader());

        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(5000)
                .build();
        request.setConfig(config);

        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() != 200) {
            return null;
        }

        String json = EntityUtils.toString(response.getEntity());
        return new JSONObject(json);
    }

    private List<JenkinsBuildStatus.StageInfo> parseStages(JSONObject buildInfo) {
        List<JenkinsBuildStatus.StageInfo> stages = new ArrayList<>();

        if (!buildInfo.has("stages")) {
            return stages;
        }

        JSONArray stagesArray = buildInfo.getJSONArray("stages");
        for (int i = 0; i < stagesArray.length(); i++) {
            JSONObject stage = stagesArray.getJSONObject(i);

            JenkinsBuildStatus.StageInfo stageInfo = new JenkinsBuildStatus.StageInfo();
            stageInfo.setStageName(stage.getString("name"));
            stageInfo.setStatus(stage.getString("status"));

            if (stage.has("durationMillis")) {
                stageInfo.setDurationMillis(stage.getLong("durationMillis"));
            }

            if (stage.has("startTimeMillis")) {
                stageInfo.setStartTime(new Date(stage.getLong("startTimeMillis")));
            }

            stages.add(stageInfo);
        }
        return stages;
    }

    private String getConsoleOutput(CloseableHttpClient client, String buildUrl) throws IOException {
        HttpGet request = new HttpGet(buildUrl);
        request.addHeader("Authorization", getAuthHeader());

        HttpResponse response = client.execute(request);
        return EntityUtils.toString(response.getEntity());
    }

    public JenkinsBuildStatus getBuildStatus(String jobName) {
        return redisTemplate.opsForValue().get(jobName);
    }

    public JenkinsBuildStatus getBuildStatusFromJenkins(String jobName) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // 1. 获取作业信息，找到最后一次构建
            String jobInfo = getJobInfo(jobName);
            JSONObject jobJson = new JSONObject(jobInfo);

            // 如果没有构建历史，返回空状态
            if (!jobJson.has("lastBuild") || jobJson.isNull("lastBuild")) {
                return null;
            }

            // 2. 获取最后一次构建的信息
            JSONObject lastBuild = jobJson.getJSONObject("lastBuild");
            // 规范化 buildUrl，使用配置的 Jenkins 地址
            String buildUrl = normalizeJenkinsUrl(lastBuild.getString("url"));

            // 3. 获取构建详情
            JSONObject buildInfo = getBuildInfo(client, buildUrl);
            if (buildInfo == null) {
                return null;
            }

            // 4. 解析构建状态
            JenkinsBuildStatus status = new JenkinsBuildStatus();
            status.setJobName(jobName);
            status.setBuildUrl(buildUrl);

            // 设置整体状态
            if (buildInfo.has("result") && !buildInfo.isNull("result")) {
                status.setOverallStatus(buildInfo.getString("result"));
            } else if (buildInfo.has("status") && !buildInfo.isNull("status")) {
                status.setOverallStatus(buildInfo.getString("status"));
            } else {
                status.setOverallStatus("IN_PROGRESS");
            }

            // 设置开始时间
            if (buildInfo.has("timestamp")) {
                long timestamp = buildInfo.getLong("timestamp");
                status.setStartTime(new Date(timestamp));
            }

            // 设置结束时间和持续时间
            if (buildInfo.has("duration")) {
                long duration = buildInfo.getLong("duration");
                if (status.getStartTime() != null) {
                    status.setEndTime(new Date(status.getStartTime().getTime() + duration));
                }
            }

            // 解析阶段信息
            List<JenkinsBuildStatus.StageInfo> stages = parseStages(buildInfo);
            status.setStages(stages);

            // 获取控制台输出（如果需要）
            try {
                String consoleOutput = getConsoleOutput(client, buildUrl + "/consoleText");
                status.setConsoleOutput(consoleOutput);
            } catch (Exception e) {
                logger.warn("获取控制台输出失败: {}", e.getMessage());
            }

            // 获取队列URL（如果构建还在队列中）
            if (status.getOverallStatus().equals("QUEUED")) {
                String queueInfo = getJobInfo(jobName);
                JSONObject queueJson = new JSONObject(queueInfo);
                if (queueJson.has("queueItem") && !queueJson.isNull("queueItem")) {
                    JSONObject queueItem = queueJson.getJSONObject("queueItem");
                    status.setQueueUrl(queueItem.getString("url"));
                }
            }

            return status;

        } catch (Exception e) {
            logger.error("从 Jenkins 获取构建状态失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取构建信息（带重试）
     */
    private JSONObject getBuildInfoWithRetry(CloseableHttpClient client, String buildUrl, int maxRetries) throws Exception {
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                return getBuildInfo(client, buildUrl);
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    Thread.sleep(2000); // 等待2秒后重试
                }
            }
        }

        throw lastException != null ? lastException : new Exception("获取构建信息失败");
    }

    /**
     * 检查构建是否正在运行
     */
    public boolean isBuildRunning(String jobName) throws Exception {
        try {
            JenkinsBuildStatus status = getBuildStatusFromJenkins(jobName);
            return status != null &&
                    (status.getOverallStatus().equals("IN_PROGRESS") ||
                            status.getOverallStatus().equals("QUEUED"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取构建控制台输出
     */
    public String getBuildConsoleOutput(String jobName) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String jobInfo = getJobInfo(jobName);
            JSONObject jobJson = new JSONObject(jobInfo);

            if (!jobJson.has("lastBuild") || jobJson.isNull("lastBuild")) {
                return null;
            }

            JSONObject lastBuild = jobJson.getJSONObject("lastBuild");
            String buildUrl = lastBuild.getString("url");

            return getConsoleOutput(client, buildUrl + "/consoleText");
        }
    }
    public String getJobInfo(String jobPath) throws IOException, URISyntaxException, JobNotFoundException {
        if (!jobExists(jobPath)) {
            throw new JobNotFoundException("Job not found: " + jobPath);
        }

        String infoUrl = buildJobUrl(jobPath) + "/api/json";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(infoUrl);
            get.addHeader("Authorization", getAuthHeader());

            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                return EntityUtils.toString(response.getEntity());
            } else {
                String errorBody = EntityUtils.toString(response.getEntity());
                throw new IOException(
                        "Get job info failed. Status: " + statusCode +
                                ", Response: " + errorBody
                );
            }
        }
    }
    // 删除 Jenkins 作业
    public boolean deleteJob(String jobPath) {
        try {
            if (!jobExists(jobPath)) {
                return false;
            }

            String deleteUrl = buildJobUrl(jobPath) + "/doDelete";
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(deleteUrl);
                post.addHeader("Authorization", getAuthHeader());

                String crumb = getCrumb();
                if (crumb != null) {
                    post.addHeader("Jenkins-Crumb", crumb);
                }

                HttpResponse response = client.execute(post);
                int statusCode = response.getStatusLine().getStatusCode();

                return statusCode == 302 || statusCode == 200;
            }
        } catch (Exception e) {
            throw new RuntimeException("删除 Jenkins 作业失败: " + e.getMessage());
        }
    }

    // 停止构建
    public boolean stopBuild(String jobPath) {
        try {
            String jobInfo = getJobInfo(jobPath);
            if (jobInfo == null) {
                return false;
            }

            JSONObject jobJson = new JSONObject(jobInfo);
            if (!jobJson.has("lastBuild") || jobJson.isNull("lastBuild")) {
                return false;
            }

            JSONObject lastBuild = jobJson.getJSONObject("lastBuild");
            String buildUrl = lastBuild.getString("url");
            String stopUrl = buildUrl + "stop";

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(stopUrl);
                post.addHeader("Authorization", getAuthHeader());

                String crumb = getCrumb();
                if (crumb != null) {
                    post.addHeader("Jenkins-Crumb", crumb);
                }

                HttpResponse response = client.execute(post);
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (Exception e) {
            throw new RuntimeException("停止构建失败: " + e.getMessage());
        }
    }

}
