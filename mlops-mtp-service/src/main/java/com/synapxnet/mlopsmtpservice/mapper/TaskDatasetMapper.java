package com.synapxnet.mlopsmtpservice.mapper;

import com.synapxnet.mlopsmtpservice.entity.TaskDataset;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TaskDatasetMapper {

    /**
     * 批量插入任务数据集
     */
    @Insert({
            "<script>",
            "INSERT INTO XnetMLops.xnet_mlops_mtp_task_dataset",
            "(uid, task_uid, dataset_id, dataset_uid, dataset_name, dataset_file, bucket_identifier)",
            "VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.uid}, #{item.task_uid}, #{item.dataset_id}, #{item.dataset_uid},",
            " #{item.dataset_name}, #{item.dataset_file}, #{item.bucket_identifier})",
            "</foreach>",
            "</script>"
    })
    int batchInsertTaskDataset(@Param("list") List<TaskDataset> taskDatasets);

    /**
     * 单个插入任务数据集
     */
    @Insert({
            "INSERT INTO XnetMLops.xnet_mlops_mtp_task_dataset",
            "(uid, task_uid, dataset_id, dataset_uid, dataset_name, dataset_file, bucket_identifier)",
            "VALUES",
            "(#{uid}, #{task_uid}, #{dataset_id}, #{dataset_uid}, #{dataset_name}, #{dataset_file}, #{bucket_identifier})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTaskDataset(TaskDataset taskDataset);

    /**
     * 根据任务UID删除数据集
     */
    @Delete("DELETE FROM XnetMLops.xnet_mlops_mtp_task_dataset WHERE task_uid = #{task_uid}")
    int deleteByTaskUid(@Param("task_uid") String taskUid);

    /**
     * 根据任务UID查找数据集
     */
    @Select("SELECT * FROM XnetMLops.xnet_mlops_mtp_task_dataset WHERE task_uid = #{task_uid}")
    List<TaskDataset> findByTaskUid(@Param("task_uid") String taskUid);

    /**
     * 检查任务是否存在数据集
     */
    @Select("SELECT COUNT(*) FROM XnetMLops.xnet_mlops_mtp_task_dataset WHERE task_uid = #{taskUid}")
    int countByTaskUid(String taskUid);

    /**
     * 清理无效的关联数据
     */
    @Delete({
            "DELETE FROM XnetMLops.xnet_mlops_mtp_task_dataset",
            "WHERE task_uid NOT IN (SELECT uid FROM XnetMLops.xnet_mlops_mtp_train_task)"
    })
    int deleteOrphanDatasets();
}