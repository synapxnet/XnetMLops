package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.HadoopVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Hadoop 版本 Mapper
 */
@Mapper
public interface HadoopVersionMapper {

    /**
     * 查询所有版本
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_hadoop_versions ORDER BY version DESC")
    List<HadoopVersion> findAll();

    /**
     * 按版本类型查询
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_hadoop_versions WHERE version_type = #{versionType} " +
            "ORDER BY version DESC")
    List<HadoopVersion> findByVersionType(@Param("versionType") String versionType);

    /**
     * 查询稳定版本
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_hadoop_versions WHERE version_type = 'stable' " +
            "ORDER BY version DESC")
    List<HadoopVersion> findStableVersions();

    /**
     * 按版本号查询
     */
    @Select("SELECT id, version, version_type as versionType, release_date as releaseDate, " +
            "download_url as downloadUrl, is_latest as isLatest, " +
            "created_at as createdAt, updated_at as updatedAt " +
            "FROM xnet_mlops_smp_hadoop_versions WHERE version = #{version}")
    HadoopVersion findByVersion(@Param("version") String version);

    /**
     * 插入版本
     */
    @Insert("INSERT INTO xnet_mlops_smp_hadoop_versions " +
            "(version, version_type, release_date, download_url, is_latest) " +
            "VALUES (#{version}, #{versionType}, #{releaseDate}, #{downloadUrl}, #{isLatest})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HadoopVersion hadoopVersion);

    /**
     * 更新版本
     */
    @Update("UPDATE xnet_mlops_smp_hadoop_versions SET " +
            "release_date = #{releaseDate}, download_url = #{downloadUrl}, " +
            "is_latest = #{isLatest} WHERE version = #{version}")
    int update(HadoopVersion hadoopVersion);

    /**
     * 重置最新版本标记
     */
    @Update("UPDATE xnet_mlops_smp_hadoop_versions SET is_latest = 0 WHERE version_type = #{versionType}")
    int resetLatestFlag(@Param("versionType") String versionType);

    /**
     * 统计版本数量
     */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_hadoop_versions WHERE version_type = #{versionType}")
    int countByVersionType(@Param("versionType") String versionType);

    /**
     * 记录同步日志
     */
    @Insert("INSERT INTO xnet_mlops_smp_hadoop_version_sync_log " +
            "(sync_type, sync_status, versions_count, error_message, sync_duration_ms) " +
            "VALUES (#{syncType}, #{syncStatus}, #{versionsCount}, #{errorMessage}, #{syncDurationMs})")
    int insertSyncLog(@Param("syncType") String syncType, @Param("syncStatus") String syncStatus,
                      @Param("versionsCount") int versionsCount, @Param("errorMessage") String errorMessage,
                      @Param("syncDurationMs") long syncDurationMs);
}
