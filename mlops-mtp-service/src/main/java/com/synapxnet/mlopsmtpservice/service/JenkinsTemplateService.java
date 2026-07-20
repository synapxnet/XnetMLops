package com.synapxnet.mlopsmtpservice.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JenkinsTemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public String generatePipelineConfig(Map<String, String> parameters) throws IOException {
        // 1. 加载模板文件
        ClassPathResource resource = new ClassPathResource("jenkins-templates/pipeline-template.xml");
        String template;
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            template = new String(bytes, StandardCharsets.UTF_8);
        }

        // 2. 替换占位符
        return replacePlaceholders(template, parameters);
    }

    private String replacePlaceholders(String template, Map<String, String> parameters) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = parameters.getOrDefault(key, "");

            // 特殊处理：如果参数值包含特殊字符，进行XML转义
            if (replacement.contains("<") || replacement.contains(">") || replacement.contains("&")) {
                replacement = escapeXml(replacement);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String escapeXml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
