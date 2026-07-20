package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.HadoopVersion;
import com.synapxnet.mlopssmpservice.mapper.HadoopVersionMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hadoop 版本服务实现
 * 从 Apache Hadoop 官网获取版本列表
 */
@Service
public class HadoopVersionServiceImpl implements HadoopVersionService {

    private static final Logger logger = LoggerFactory.getLogger(HadoopVersionServiceImpl.class);

    // Apache Hadoop 官网镜像 URL
    private static final String HADOOP_ARCHIVE_URL = "https://archive.apache.org/dist/hadoop/common/";
    
    // 国内镜像源（备用）
    private static final String[] MIRROR_URLS = {
            "https://archive.apache.org/dist/hadoop/common/",
            "https://mirrors.tuna.tsinghua.edu.cn/apache/hadoop/common/",
            "https://mirrors.huaweicloud.com/apache/hadoop/common/"
    };

    // 版本号正则 (匹配 hadoop-3.x.x 格式)
    private static final Pattern VERSION_PATTERN = Pattern.compile("^hadoop-(\\d+\\.\\d+\\.\\d+)/?$");

    @Autowired
    private HadoopVersionMapper hadoopVersionMapper;

    @Override
    public List<HadoopVersion> getAllVersions() {
        List<HadoopVersion> versions = hadoopVersionMapper.findAll();
        if (versions == null || versions.isEmpty()) {
            logger.info("数据库中无版本数据，尝试从官网获取...");
            refreshVersions();
            versions = hadoopVersionMapper.findAll();
        }
        return versions != null ? versions : Collections.emptyList();
    }

    @Override
    public List<HadoopVersion> getStableVersions() {
        List<HadoopVersion> versions = hadoopVersionMapper.findStableVersions();
        if (versions == null || versions.isEmpty()) {
            logger.info("数据库中无稳定版本数据，尝试从官网获取...");
            refreshVersions();
            versions = hadoopVersionMapper.findStableVersions();
        }
        return versions != null ? versions : Collections.emptyList();
    }

    @Override
    @Transactional
    public Map<String, Object> refreshVersions() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int totalCount = 0;
        String errorMessage = null;

        try {
            logger.info("开始从 Apache 官网刷新 Hadoop 版本列表...");

            // 尝试从多个镜像源获取
            List<HadoopVersion> versions = null;
            String usedMirror = null;

            for (String mirrorUrl : MIRROR_URLS) {
                try {
                    logger.info("尝试镜像源: {}", mirrorUrl);
                    versions = fetchVersionsFromMirror(mirrorUrl);
                    if (versions != null && !versions.isEmpty()) {
                        usedMirror = mirrorUrl;
                        break;
                    }
                } catch (Exception e) {
                    logger.warn("镜像源 {} 获取失败: {}", mirrorUrl, e.getMessage());
                }
            }

            if (versions == null || versions.isEmpty()) {
                throw new RuntimeException("所有镜像源都无法获取版本列表");
            }

            logger.info("从 {} 获取到 {} 个版本", usedMirror, versions.size());

            // 重置最新版本标记
            hadoopVersionMapper.resetLatestFlag("stable");

            // 标记最新版本
            if (!versions.isEmpty()) {
                versions.get(0).setIsLatest(true);
            }

            // 保存到数据库
            for (HadoopVersion version : versions) {
                HadoopVersion existing = hadoopVersionMapper.findByVersion(version.getVersion());

                if (existing != null) {
                    // 更新
                    version.setId(existing.getId());
                    hadoopVersionMapper.update(version);
                } else {
                    // 插入
                    hadoopVersionMapper.insert(version);
                }
                totalCount++;
            }

            result.put("success", true);
            result.put("count", totalCount);
            result.put("mirror", usedMirror);
            result.put("message", "成功同步 " + totalCount + " 个版本");

        } catch (Exception e) {
            logger.error("刷新版本列表失败: {}", e.getMessage(), e);
            errorMessage = e.getMessage();
            result.put("success", false);
            result.put("message", "刷新失败: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        result.put("durationMs", duration);

        // 记录同步日志
        try {
            hadoopVersionMapper.insertSyncLog(
                    "stable",
                    errorMessage == null ? "success" : "failed",
                    totalCount,
                    errorMessage,
                    duration
            );
        } catch (Exception e) {
            logger.warn("记录同步日志失败: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getVersionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("stableCount", hadoopVersionMapper.countByVersionType("stable"));

        // 获取最新版本
        List<HadoopVersion> versions = hadoopVersionMapper.findStableVersions();
        if (versions != null && !versions.isEmpty()) {
            stats.put("latestVersion", versions.get(0).getVersion());
        }

        return stats;
    }

    /**
     * 从镜像站获取版本列表
     */
    private List<HadoopVersion> fetchVersionsFromMirror(String mirrorUrl) throws Exception {
        List<HadoopVersion> versions = new ArrayList<>();

        // 使用 Jsoup 解析 HTML 目录页面
        Document doc = Jsoup.connect(mirrorUrl)
                .timeout(30000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get();

        // 解析目录列表
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("href");
            Matcher matcher = VERSION_PATTERN.matcher(href);

            if (matcher.matches()) {
                String versionStr = matcher.group(1);
                
                // 只获取 3.x 版本（稳定版）
                if (!versionStr.startsWith("3.")) {
                    continue;
                }

                HadoopVersion version = new HadoopVersion();
                version.setVersion(versionStr);
                version.setVersionType("stable");
                version.setDownloadUrl(mirrorUrl + "hadoop-" + versionStr + "/hadoop-" + versionStr + ".tar.gz");
                version.setIsLatest(false);

                // 尝试从页面获取日期信息
                try {
                    Element parent = link.parent();
                    if (parent != null) {
                        String text = parent.text();
                        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
                        Matcher dateMatcher = datePattern.matcher(text);
                        if (dateMatcher.find()) {
                            version.setReleaseDate(LocalDate.parse(dateMatcher.group(1)));
                        }
                    }
                } catch (Exception e) {
                    // 日期解析失败，忽略
                }

                versions.add(version);
            }
        }

        // 按版本号排序（降序）
        versions.sort((v1, v2) -> compareVersions(v2.getVersion(), v1.getVersion()));

        return versions;
    }

    /**
     * 比较版本号
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }
}
