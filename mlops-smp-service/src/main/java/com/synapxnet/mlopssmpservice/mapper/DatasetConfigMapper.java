package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.DatasetConfig;
import org.apache.ibatis.annotations.*;



import java.util.List;
import java.util.Map;

@Mapper
public interface DatasetConfigMapper {
    @Select("SELECT label, value FROM xnet_mlops_smp_dataset_config_info WHERE config_type = 'DATASET_TYPE'")
    List<Map<String, String>> getDatasetTypes();

    @Select("SELECT label, value FROM xnet_mlops_smp_dataset_config_info WHERE config_type = 'DATASET_ZONE'")
    List<Map<String, String>> getDatasetZones();

    // 新增数据插入方法
    @Insert("INSERT INTO xnet_mlops_smp_dataset_config_info(label, value, config_type) " +
            "VALUES(#{label}, #{value}, #{configType})")
    int insertDatasetConfig(@Param("label") String label,
                            @Param("value") String value,
                            @Param("configType") String configType);

    // 添加删除方法
    @Delete("DELETE FROM xnet_mlops_smp_dataset_config_info " +
            "WHERE config_type = #{configType} AND value = #{value}")
    int deleteDatasetConfig(
            @Param("configType") String configType,
            @Param("value") String value
    );

}
