package com.synapxnet.mlopssmpservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作站点实体类
 * 用于存储服务器注册信息
 */
@Data
public class Workstation {

    /** 主键ID */
    private Long id;

    /** 唯一标识符(UUID) */
    private String uid;

    /** 服务器名称(唯一) */
    private String name;

    /** 服务器主机名(用于Hadoop集群等) */
    private String hostname;

    /** 主机名模式: auto(自动获取)/custom(自定义) */
    private String hostnameMode;

    // ==================== 服务器基本信息 ====================

    /** 服务器厂商: huawei/tencent/alibaba/aws/azure/other */
    private String vendor;

    /** 服务器类型: vps/cce/cvm/ecs/ec2/other */
    private String serverType;

    /** 地域 */
    private String region;

    // ==================== 操作系统信息 ====================

    /** 系统类型: linux/macos/windows */
    private String osType;

    /** 系统版本 */
    private String osVersion;

    // ==================== 服务器资源 ====================

    /** CPU核数 */
    private Integer cpuCores;

    /** 内存(GB) */
    private Integer ramGb;

    /** 磁盘空间(GB) */
    private Integer diskGb;

    // ==================== GPU 资源 ====================

    /** 是否有GPU */
    private Boolean hasGpu;

    /** GPU数量 */
    private Integer gpuCount;

    /** GPU类型: single_gpu/multi_gpu */
    private String gpuType;

    /** GPU型号: NVIDIA A100/V100/RTX 3090等 */
    private String gpuModel;

    /** GPU显存(GB) */
    private Integer gpuMemory;

    /** 可用磁盘空间(GB) - 自动检测 */
    private Integer availableDiskGb;

    /** 可用内存(GB) - 自动检测 */
    private Integer availableRamGb;

    // ==================== 网络信息 ====================

    /** 域名(可选) */
    private String domain;

    /** IP地址 */
    private String ipAddress;

    // ==================== SSH连接配置 ====================

    /** SSH端口 */
    private Integer sshPort;

    /** SSH用户名 */
    private String sshUser;

    /** 认证方式: password/privateKey */
    private String authType;

    /** SSH密码(AES加密存储) - 不返回给前端 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String encryptedPassword;

    /** SSH私钥(AES加密存储) - 不返回给前端 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String encryptedPrivateKey;

    // ==================== 状态信息 ====================

    /** 状态: pending/online/offline/error */
    private String status;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 最后检测结果(JSON) */
    private String lastCheckResult;

    // ==================== 审计字段 ====================

    /** 描述 */
    private String description;

    /** 创建者 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ==================== 非持久化字段(仅用于传输) ====================

    /** SSH明文密码(仅用于创建/更新时传输，不存储到数据库) */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** SSH明文私钥(仅用于创建/更新时传输，不存储到数据库) */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String privateKey;

    /** 测试连接是否通过(用于前端状态) */
    private transient Boolean connectionTested;
}
