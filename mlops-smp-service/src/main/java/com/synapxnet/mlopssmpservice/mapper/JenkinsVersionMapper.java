package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.JenkinsVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Jenkins 版本 Mapper
 */
@Mapper
public interface JenkinsVersionMapper {

    /**
     * 查询所有版本
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, sha256, is_lts as isLts, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_jenkins_versions ORDER BY release_date DESC, version DESC")
    List<JenkinsVersion> findAll();

    /**
     * 按版本类型查询
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, sha256, is_lts as isLts, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_jenkins_versions WHERE version_type = #{versionType} " +
            "ORDER BY release_date DESC, version DESC")
    List<JenkinsVersion> findByVersionType(@Param("versionType") String versionType);

    /**
     * 查询LTS版本
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, sha256, is_lts as isLts, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_jenkins_versions WHERE is_lts = 1 " +
            "ORDER BY release_date DESC, version DESC")
    List<JenkinsVersion> findLtsVersions();

    /**
     * 按版本号查询
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, sha256, is_lts as isLts, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_jenkins_versions WHERE version = #{version} AND version_type = #{versionType}")
    JenkinsVersion findByVersionAndType(@Param("version") String version, @Param("versionType") String versionType);

    /**
     * 插入版本
     */
    @Insert("INSERT INTO xnet_mlops_smp_jenkins_versions " +
            "(version, version_type, release_date, download_url, sha256, is_lts, is_latest) " +
            "VALUES (#{version}, #{versionType}, #{releaseDate}, #{downloadUrl}, #{sha256}, #{isLts}, #{isLatest})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JenkinsVersion jenkinsVersion);

    /**
     * 更新版本
     */
    @Update("UPDATE xnet_mlops_smp_jenkins_versions SET " +
            "release_date = #{releaseDate}, download_url = #{downloadUrl}, sha256 = #{sha256}, " +
            "is_lts = #{isLts}, is_latest = #{isLatest} " +
            "WHERE version = #{version} AND version_type = #{versionType}")
    int update(JenkinsVersion jenkinsVersion);

    /**
     * 重置最新版本标记
     */
    @Update("UPDATE xnet_mlops_smp_jenkins_versions SET is_latest = 0 WHERE version_type = #{versionType}")
    int resetLatestFlag(@Param("versionType") String versionType);

    /**
     * 删除所有版本
     */
    @Delete("DELETE FROM xnet_mlops_smp_jenkins_versions WHERE version_type = #{versionType}")
    int deleteByVersionType(@Param("versionType") String versionType);

    /**
     * 统计版本数量
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_jenkins_versions WHERE version_type = #{versionType}")
    int countByVersionType(@Param("versionType") String versionType);

    /**
     * 记录同步日志
     */
    @Insert("INSERT INTO xnet_mlops_smp_jenkins_version_sync_log " +
            "(sync_type, sync_status, versions_count, error_message, sync_duration_ms) " +
            "VALUES (#{syncType}, #{syncStatus}, #{versionsCount}, #{errorMessage}, #{syncDurationMs})")
    int insertSyncLog(@Param("syncType") String syncType, @Param("syncStatus") String syncStatus,
                      @Param("versionsCount") int versionsCount, @Param("errorMessage") String errorMessage,
                      @Param("syncDurationMs") long syncDurationMs);
}
