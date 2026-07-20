package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.PipelineConfigParams;
import com.synapxnet.mlopssmpservice.exception.JobNotFoundException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JenkinsService {

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String username;

    @Value("${jenkins.api-token}")
    private String apiToken;

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
            // 简单解析 JSON
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

    // 构建作业URL（支持多级文件夹）
    private String buildJobUrl(String jobPath) throws URISyntaxException {
        // 确保基础URL以斜杠结尾
        String baseUrl = jenkinsUrl.endsWith("/") ? jenkinsUrl : jenkinsUrl + "/";

        // 分割路径段
        String[] pathSegments = jobPath.split("/");
        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        // 为每个路径段添加/job/前缀
        for (String segment : pathSegments) {
            urlBuilder.append("job/")
                    .append(URLEncoder.encode(segment, StandardCharsets.UTF_8))
                    .append("/");
        }

        // 移除最后一个斜杠
        return urlBuilder.substring(0, urlBuilder.length() - 1);
    }

    // 创建作业方法（支持文件夹路径）
    public String createJob(String jobPath, String jobConfig) throws IOException, URISyntaxException {
        // 解析文件夹路径和作业名
        int lastSlashIndex = jobPath.lastIndexOf('/');
        String jobName = (lastSlashIndex != -1) ? jobPath.substring(lastSlashIndex + 1) : jobPath;
        String folderPath = (lastSlashIndex != -1) ? jobPath.substring(0, lastSlashIndex) : "";

        // 构建正确的创建URL
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

            // 添加 CSRF Crumb
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

    // 创建Docker流水线作业方法
    public String createDockerPipelineJob(String jobName, PipelineConfigParams params)
            throws IOException, URISyntaxException {

        // 准备模板参数
        Map<String, String> templateParams = new HashMap<>();

        // Docker构建相关参数
        templateParams.put("dockerImageName", params.getDockerImageName());

        // 将'-'分隔的标签转换为逗号分隔
        if (params.getDockerImageTags() != null) {
            String tags = params.getDockerImageTags().replace("-", ",");
            templateParams.put("dockerImageTags", tags);
        } else {
            templateParams.put("dockerImageTags", "");
        }

        templateParams.put("harborUrl", params.getHarborUrl());
        templateParams.put("dockerfileContent", params.getDockerfileContent());
        templateParams.put("harborCredentialsId", params.getHarborCredentialsId());

        // 可选参数（如有需要）
        templateParams.put("description", params.getDescription() != null ?
                params.getDescription() : "Docker镜像构建流水线");

        // 生成动态配置 - 使用Docker流水线模板
        String jobConfig = generateDockerPipelineConfig(templateParams);

        // 创建作业
        String createResult = createJob(jobName, jobConfig);

        // 创建成功后立即触发构建
        try {
            triggerBuild(jobName);
            return createResult + " | Docker build pipeline triggered successfully!";
        } catch (Exception e) {
            return createResult + " | Pipeline created but failed to trigger: " + e.getMessage();
        }
    }

    // Docker流水线模板处理逻辑
    private String generateDockerPipelineConfig(Map<String, String> parameters) throws IOException {
        // 加载Docker流水线模板文件
        ClassPathResource resource = new ClassPathResource("jenkins-templates/docker-pipeline-template.xml");
        String template;
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            template = new String(bytes, StandardCharsets.UTF_8);
        }

        // 替换占位符
        return replacePlaceholders(template, parameters);
    }

    // 占位符替换逻辑
    private String replacePlaceholders(String template, Map<String, String> parameters) {
        // 第一阶段：替换XML部分的参数（非CDATA）
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            template = template.replace(placeholder,
                    escapeXml(entry.getValue() != null ? entry.getValue() : ""));
        }

        // 第二阶段：处理CDATA部分
        String cdataStart = "<![CDATA[";
        String cdataEnd = "]]>";
        int start = template.indexOf(cdataStart);
        int end = template.indexOf(cdataEnd);

        if (start != -1 && end != -1) {
            String cdataContent = template.substring(start + cdataStart.length(), end);

            // 替换所有参数占位符
            for (String param : parameters.keySet()) {
                String placeholder = "${" + param + "}";
                String value = parameters.get(param) != null ?
                        parameters.get(param) : "";
                cdataContent = cdataContent.replace(placeholder, value);
            }

            // 重新组装模板
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
            int statusCode = response.getStatusLine().getStatusCode();

            // 200 表示作业存在，404 表示不存在
            return statusCode == HttpStatus.SC_OK;
        }
    }

    // 触发构建（支持多级文件夹路径）
    public void triggerBuild(String jobPath) throws IOException, URISyntaxException, JobNotFoundException {
        // 1. 检查作业是否存在
        if (!jobExists(jobPath)) {
            throw new JobNotFoundException("Job not found: " + jobPath);
        }

        // 2. 构建触发URL - 使用 buildWithParameters 端点更可靠
        String buildUrl = buildJobUrl(jobPath) + "/buildWithParameters";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(buildUrl);
            post.addHeader("Authorization", getAuthHeader());

            // 3. 使用 XML 格式的请求体（Jenkins 更偏好）
            post.setEntity(new StringEntity("<run/>", ContentType.APPLICATION_XML));

            // 4. 添加 CSRF Crumb（双重确认）
            String crumb = getCrumb();
            if (crumb != null) {
                post.addHeader("Jenkins-Crumb", crumb);
            }

            // 5. 添加必要的认证头
            post.addHeader("Accept", "application/json");

            // 6. 触发构建
            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();

            // 注意：成功响应码是 201 (SC_CREATED)
            if (statusCode != HttpStatus.SC_CREATED) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new IOException("Trigger failed. Status: " + statusCode +
                        ", Response: " + responseBody);
            }
        }
    }

    // 获取作业信息（支持多级文件夹路径）
    public String getJobInfo(String jobPath) throws IOException, URISyntaxException, JobNotFoundException {
        // 先检查作业是否存在
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
}