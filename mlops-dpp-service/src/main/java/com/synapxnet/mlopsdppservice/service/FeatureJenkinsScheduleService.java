package com.synapxnet.mlopsdppservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsdppservice.entity.FeaturePipelineParams;
import com.synapxnet.mlopsdppservice.entity.FeatureTaskInfo;
import com.synapxnet.mlopsdppservice.entity.JenkinsBuildStatus;
import com.synapxnet.mlopsdppservice.exception.JobNotFoundException;
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
import java.util.concurrent.TimeUnit;

@Service
public class FeatureJenkinsScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureJenkinsScheduleService.class);

    @Autowired(required = false)
    @Qualifier("featureJenkinsBuildStatusRedisTemplate")
    private RedisTemplate<String, JenkinsBuildStatus> redisTemplate;

    @Autowired
    private FeatureTaskInfoService featureTaskInfoService;

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
     * 规范化 Jenkins URL
     */
    private String normalizeJenkinsUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        try {
            URI originalUri = new URI(url);
            URI configuredUri = new URI(jenkinsUrl);
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

    // 构建作业URL
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

    // 检查文件夹是否存在
    private boolean folderExists(String folderPath) throws IOException, URISyntaxException {
        String folderUrl = buildJobUrl(folderPath) + "/api/json";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpHead head = new HttpHead(folderUrl);
            head.addHeader("Authorization", getAuthHeader());

            HttpResponse response = client.execute(head);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        }
    }

    // 创建 Jenkins 文件夹
    private void createFolder(String folderName, String parentPath) throws IOException, URISyntaxException {
        String createUrl;
        if (parentPath != null && !parentPath.isEmpty()) {
            createUrl = buildJobUrl(parentPath) + "/createItem";
        } else {
            createUrl = jenkinsUrl + "/createItem";
        }

        // Jenkins 文件夹配置 XML
        String folderConfig = "<?xml version='1.1' encoding='UTF-8'?>\n" +
                "<com.cloudbees.hudson.plugins.folder.Folder plugin=\"cloudbees-folder@6.858.v898218f3571d\">\n" +
                "  <description></description>\n" +
                "  <properties/>\n" +
                "  <folderViews class=\"com.cloudbees.hudson.plugins.folder.views.DefaultFolderViewHolder\">\n" +
                "    <views>\n" +
                "      <hudson.model.AllView>\n" +
                "        <owner class=\"com.cloudbees.hudson.plugins.folder.Folder\" reference=\"../../../..\"/>\n" +
                "        <name>All</name>\n" +
                "        <filterExecutors>false</filterExecutors>\n" +
                "        <filterQueue>false</filterQueue>\n" +
                "        <properties class=\"hudson.model.View$PropertyList\"/>\n" +
                "      </hudson.model.AllView>\n" +
                "    </views>\n" +
                "    <tabBar class=\"hudson.views.DefaultViewsTabBar\"/>\n" +
                "  </folderViews>\n" +
                "  <healthMetrics/>\n" +
                "  <icon class=\"com.cloudbees.hudson.plugins.folder.icons.StockFolderIcon\"/>\n" +
                "</com.cloudbees.hudson.plugins.folder.Folder>";

        URI uri = new URIBuilder(createUrl)
                .addParameter("name", folderName)
                .build();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(uri);
            post.addHeader("Content-Type", "application/xml;charset=UTF-8");
            post.setEntity(new StringEntity(folderConfig, StandardCharsets.UTF_8));
            post.addHeader("Authorization", getAuthHeader());

            String crumb = getCrumb();
            if (crumb != null) {
                post.addHeader("Jenkins-Crumb", crumb);
            }

            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK && statusCode != 302) {
                String errorBody = EntityUtils.toString(response.getEntity());
                // 如果文件夹已存在，忽略错误
                if (!errorBody.contains("already exists")) {
                    throw new IOException("Failed to create folder. Status: " + statusCode + ", Response: " + errorBody);
                }
            }
            logger.info("文件夹已创建或已存在: {}", (parentPath != null && !parentPath.isEmpty()) ? parentPath + "/" + folderName : folderName);
        }
    }

    // 确保文件夹路径存在
    private void ensureFolderPath(String folderPath) throws IOException, URISyntaxException {
        if (folderPath == null || folderPath.isEmpty()) {
            return;
        }

        String[] segments = folderPath.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String segment : segments) {
            String parentPath = currentPath.length() > 0 ? currentPath.toString() : null;

            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(segment);

            // 检查文件夹是否存在
            if (!folderExists(currentPath.toString())) {
                logger.info("创建 Jenkins 文件夹: {}", currentPath.toString());
                createFolder(segment, parentPath);
            }
        }
    }

    // 创建作业方法
    public String createJob(String jobPath, String jobConfig) throws IOException, URISyntaxException {
        int lastSlashIndex = jobPath.lastIndexOf('/');
        String jobName = (lastSlashIndex != -1) ? jobPath.substring(lastSlashIndex + 1) : jobPath;
        String folderPath = (lastSlashIndex != -1) ? jobPath.substring(0, lastSlashIndex) : "";

        // 确保父文件夹存在
        if (!folderPath.isEmpty()) {
            ensureFolderPath(folderPath);
        }

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
                throw new IOException("Failed to create job. Status: " + statusCode + ", Response: " + errorBody);
            }
        }
    }

    // 动态创建调度作业
    public String createDynamicJob(String jobName, FeaturePipelineParams params) throws IOException, URISyntaxException {
        Map<String, String> templateParams = new HashMap<>();

        templateParams.put("description", params.getDescription() != null ? params.getDescription() : "");
        templateParams.put("taskName", params.getTaskName() != null ? params.getTaskName() : "");
        templateParams.put("taskUid", params.getTaskUid() != null ? params.getTaskUid() : "");

        // 数据源配置
        templateParams.put("datasourceName", params.getDatasourceName() != null ? params.getDatasourceName() : "");
        templateParams.put("database", params.getDatabase() != null ? params.getDatabase() : "");
        templateParams.put("tableName", params.getTableName() != null ? params.getTableName() : "");
        templateParams.put("selectedColumns", params.getSelectedColumns() != null ? params.getSelectedColumns() : "");

        // 特征工程配置
        templateParams.put("operatorCode", params.getOperatorCode() != null ? params.getOperatorCode() : "");
        templateParams.put("operatorName", params.getOperatorName() != null ? params.getOperatorName() : "");
        templateParams.put("outputFormat", params.getOutputFormat() != null ? params.getOutputFormat() : "csv");
        templateParams.put("featureConfig", params.getFeatureConfig() != null ? params.getFeatureConfig() : "{}");
        templateParams.put("operatorParams", params.getOperatorParams() != null ? params.getOperatorParams() : "{}");

        // 输出配置
        templateParams.put("pushToDataset", String.valueOf(params.getPushToDataset() != null && params.getPushToDataset()));
        templateParams.put("targetDatasetName", params.getTargetDatasetName() != null ? params.getTargetDatasetName() : "");
        templateParams.put("outputPath", params.getOutputPath() != null ? params.getOutputPath() : "");
        templateParams.put("encryption", String.valueOf(params.getEncryption() != null && params.getEncryption()));

        // 存储配置
        templateParams.put("zone", params.getZone() != null ? params.getZone() : "");
        templateParams.put("bucketUid", params.getBucketUid() != null ? params.getBucketUid() : "");
        templateParams.put("bucketName", params.getBucketName() != null ? params.getBucketName() : "");

        // Docker镜像配置
        templateParams.put("dockerImageName", params.getDockerImageName() != null ? params.getDockerImageName() : "");
        templateParams.put("dockerImageTag", params.getDockerImageTag() != null ? params.getDockerImageTag() : "latest");
        templateParams.put("harborUrl", params.getHarborUrl() != null ? params.getHarborUrl() : "");
        templateParams.put("harborCredentialsId", params.getHarborCredentialsId() != null ? params.getHarborCredentialsId() : "");

        // 调度配置 - 调度任务强制启用调度
        if (params.getScheduleConfig() != null && !params.getScheduleConfig().isEmpty()) {
            boolean scheduleEnabled = scheduleService.isScheduleActive(params.getScheduleConfig());
            String cronExpression = scheduleService.convertToCronExpression(params.getScheduleConfig());

            templateParams.put("SCHEDULE_ENABLED", String.valueOf(scheduleEnabled));
            templateParams.put("CRON_EXPRESSION", cronExpression);

            logger.info("调度配置解析结果 - enabled: {}, cron: {}", scheduleEnabled, cronExpression);
        } else {
            templateParams.put("SCHEDULE_ENABLED", "true");
            templateParams.put("CRON_EXPRESSION", "0 0 * * *"); // 默认每天凌晨执行
        }

        String jobConfig = generatePipelineConfig(templateParams);
        return createJob(jobName, jobConfig);
    }

    // 模板处理逻辑
    private String generatePipelineConfig(Map<String, String> parameters) throws IOException {
        ClassPathResource resource = new ClassPathResource("jenkins-templates/feature-pipeline-template.xml");
        String template;
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            template = new String(bytes, StandardCharsets.UTF_8);
        }

        logger.info("Feature Pipeline template parameters: {}", parameters);
        return replacePlaceholders(template, parameters);
    }

    // 占位符替换逻辑
    private String replacePlaceholders(String template, Map<String, String> parameters) {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            template = template.replace(placeholder, escapeXml(entry.getValue() != null ? entry.getValue() : ""));
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

            template = template.substring(0, start) + cdataStart + cdataContent + cdataEnd + template.substring(end + cdataEnd.length());
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

    // 触发构建
    public String triggerBuild(String jobPath, String parentTaskUID) throws IOException, URISyntaxException, JobNotFoundException {
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
                throw new IOException("Trigger failed. Status: " + statusCode + ", Response: " + responseBody);
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

    // 跟踪构建状态
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

            boolean buildCompleted = false;
            int consecutiveFailures = 0;
            final int MAX_CONSECUTIVE_FAILURES = 5;
            final long TIMEOUT = 30 * 60 * 1000;
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

                    consecutiveFailures = 0;

                    List<JenkinsBuildStatus.StageInfo> stages = parseStages(buildInfo);
                    currentStatus.setStages(stages);

                    if (buildInfo.has("timestamp")) {
                        long timestamp = buildInfo.getLong("timestamp");
                        currentStatus.setStartTime(new Date(timestamp));
                    }

                    if (buildInfo.has("result") && !buildInfo.isNull("result")) {
                        String result = buildInfo.getString("result");
                        currentStatus.setOverallStatus(result);

                        try {
                            currentStatus.setConsoleOutput(getConsoleOutput(client, buildUrl));
                        } catch (Exception e) {
                            logger.error("Failed to get console output: {}", e.getMessage());
                        }

                        if (buildInfo.has("duration")) {
                            long duration = buildInfo.getLong("duration");
                            currentStatus.setEndTime(new Date(currentStatus.getStartTime().getTime() + duration));
                        }

                        saveBuildStatus(jobPath, currentStatus);
                        logger.info("Build completed for job: {} with status: {}", jobPath, result);
                        buildCompleted = true;
                    } else {
                        String consoleText = getConsoleOutput(client, buildUrl + "/consoleText");

                        if (consoleText.contains("Finished: SUCCESS") ||
                                consoleText.contains("Finished: FAILURE") ||
                                consoleText.contains("Finished: ABORTED")) {

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

            if (!buildCompleted) {
                logger.error("Build status tracking timed out for job: {}", jobPath);
                updateStatus(jobPath, "TIMEOUT", "Build did not complete within timeout");
            }
        } catch (Exception e) {
            logger.error("Error tracking build status for job: " + jobPath, e);
            updateStatus(jobPath, "ERROR", "Error tracking build: " + e.getMessage());
        }
    }

    // 保存或更新任务信息到数据库 - 调度任务
    private void saveOrUpdateTaskInfo(String jobPath, JenkinsBuildStatus status) {
        try {
            if (status.getParentTaskUID() == null || status.getParentTaskUID().isEmpty()) {
                logger.warn("保存调度任务信息时 parentTaskUID 为空, jobPath={}", jobPath);
                return;
            }

            FeatureTaskInfo taskInfo = new FeatureTaskInfo();
            taskInfo.setJobUid(jobPath);
            taskInfo.setTaskUid(status.getParentTaskUID());
            taskInfo.setJobStatus(status.getOverallStatus());
            taskInfo.setStartAt(status.getStartTime());
            taskInfo.setEndAt(status.getEndTime());
            taskInfo.setJobContent(objectMapper.writeValueAsString(status));
            taskInfo.setScheduleActive(1); // 调度执行

            logger.info("准备保存调度任务信息: jobPath={}, taskUid={}, status={}",
                    jobPath, status.getParentTaskUID(), status.getOverallStatus());
            featureTaskInfoService.saveOrUpdateTaskInfo(taskInfo);
            logger.info("调度任务信息已保存或更新: jobPath={}, status={}", jobPath, status.getOverallStatus());
        } catch (JsonProcessingException e) {
            logger.error("序列化构建状态失败", e);
        } catch (Exception e) {
            logger.error("保存调度任务信息到数据库失败: {}", e.getMessage(), e);
        }
    }

    // 保存构建状态
    private void saveBuildStatus(String jobName, JenkinsBuildStatus status) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(jobName, status, 24, TimeUnit.HOURS);
                logger.info("已保存到Redis: jobName={}", jobName);
            } else {
                logger.warn("Redis不可用，跳过Redis缓存: jobName={}", jobName);
            }
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

    // 辅助方法
    private String waitForBuildStart(CloseableHttpClient client, String queueUrl) throws IOException, InterruptedException {
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
        if (redisTemplate == null) {
            logger.warn("Redis不可用，无法获取构建状态: jobName={}", jobName);
            return null;
        }
        return redisTemplate.opsForValue().get(jobName);
    }

    public Map<String, Object> getAllBuildStatusFromJenkins(String jobName) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String jobInfo = getJobInfo(jobName);
            JSONObject jobJson = new JSONObject(jobInfo);

            if (!jobJson.has("builds") || jobJson.isNull("builds")) {
                return null;
            }

            JSONArray allBuilds = jobJson.getJSONArray("builds");
            int nextBuildNumber = jobJson.getInt("nextBuildNumber");

            List<Map<String, Object>> allBuildsList = new ArrayList<>();
            Map<String, Object> allbuildMap = new HashMap<>();

            for (int i = 0; i < allBuilds.length(); i++) {
                Map<String, Object> buildMap = new HashMap<>();
                JSONObject build = allBuilds.getJSONObject(i);

                String buildUrl = normalizeJenkinsUrl(build.getString("url"));
                int buildNumber = build.getInt("number");

                JSONObject buildInfo = getBuildInfo(client, buildUrl);

                JenkinsBuildStatus status = new JenkinsBuildStatus();
                status.setJobName(jobName);
                status.setBuildUrl(buildUrl);

                if (buildInfo.has("result") && !buildInfo.isNull("result")) {
                    status.setOverallStatus(buildInfo.getString("result"));
                } else if (buildInfo.has("status") && !buildInfo.isNull("status")) {
                    status.setOverallStatus(buildInfo.getString("status"));
                } else {
                    status.setOverallStatus("IN_PROGRESS");
                }

                if (buildInfo.has("timestamp")) {
                    long timestamp = buildInfo.getLong("timestamp");
                    status.setStartTime(new Date(timestamp));
                }

                if (buildInfo.has("duration")) {
                    long duration = buildInfo.getLong("duration");
                    if (status.getStartTime() != null) {
                        status.setEndTime(new Date(status.getStartTime().getTime() + duration));
                    }
                }

                List<JenkinsBuildStatus.StageInfo> stages = parseStages(buildInfo);
                status.setStages(stages);

                try {
                    String consoleOutput = getConsoleOutput(client, buildUrl + "/consoleText");
                    status.setConsoleOutput(consoleOutput);
                } catch (Exception e) {
                    logger.warn("获取控制台输出失败: {}", e.getMessage());
                }

                buildMap.put("buildNumber", buildNumber);
                buildMap.put("jobContent", status);
                allBuildsList.add(buildMap);
            }

            allbuildMap.put("nextBuildNumber", nextBuildNumber);
            allbuildMap.put("jobHistoryBuild", allBuildsList);

            return allbuildMap;

        } catch (Exception e) {
            logger.error("从 Jenkins 获取构建状态失败: {}", e.getMessage());
            throw e;
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
                throw new IOException("Get job info failed. Status: " + statusCode + ", Response: " + errorBody);
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
