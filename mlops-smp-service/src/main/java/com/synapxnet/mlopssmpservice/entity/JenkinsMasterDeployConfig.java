package com.synapxnet.mlopssmpservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * Jenkins Master 部署配置类
 * 用于传递部署参数
 */
@Data
public class JenkinsMasterDeployConfig {

    // ==================== 基础配置 ====================

    /** Jenkins版本 (如: 2.426.3) */
    private String jenkinsVersion;

    /** Jenkins HTTP端口 (默认8080) */
    private Integer jenkinsPort;

    /** Jenkins Home目录 (默认/var/jenkins_home) */
    private String jenkinsHome;

    /** Java版本 (11, 17, 21) */
    private String javaVersion;

    /** JVM参数 (如: -Xmx2g -Xms1g) */
    private String javaOpts;

    /** 时区 (如: Asia/Shanghai) */
    private String timezone;

    // ==================== 管理员配置 ====================

    /** 管理员用户名 */
    private String adminUsername;

    /** 管理员密码 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String adminPassword;

    /** 管理员邮箱 */
    private String adminEmail;

    // ==================== 插件配置 ====================

    /** 是否安装推荐插件 */
    private Boolean installSuggestedPlugins;

    /** 额外安装的插件列表 */
    private List<String> additionalPlugins;

    // ==================== 凭证配置 ====================

    /** Git凭证列表 */
    private List<GitCredential> gitCredentials;

    /** Harbor凭证列表 */
    private List<HarborCredential> harborCredentials;

    /** SSH凭证列表 */
    private List<SSHCredential> sshCredentials;

    // ==================== 安全配置 ====================

    /** 是否启用CSRF保护 */
    private Boolean enableCsrf;

    /** 是否启用Agent到Master安全 */
    private Boolean enableAgentToMasterSecurity;

    // ==================== 内部类: 凭证定义 ====================

    /**
     * Git凭证
     */
    @Data
    public static class GitCredential {
        /** 凭证ID */
        private String id;
        /** 描述 */
        private String description;
        /** 用户名 */
        private String username;
        /** 密码或Token */
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String password;
    }

    /**
     * Harbor凭证
     */
    @Data
    public static class HarborCredential {
        /** 凭证ID */
        private String id;
        /** 描述 */
        private String description;
        /** Harbor URL */
        private String url;
        /** 用户名 */
        private String username;
        /** 密码 */
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String password;
    }

    /**
     * SSH凭证
     */
    @Data
    public static class SSHCredential {
        /** 凭证ID */
        private String id;
        /** 描述 */
        private String description;
        /** 用户名 */
        private String username;
        /** 私钥内容 */
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String privateKey;
        /** 私钥密码(可选) */
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String passphrase;
    }
}
