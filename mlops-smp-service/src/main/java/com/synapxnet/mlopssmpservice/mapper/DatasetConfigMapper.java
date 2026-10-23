package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.DatasetConfig;
import org.apache.ibatis.annotations.*;



import java.util.List;
import java.util.Map;

@Mapper
public interface DatasetConfigMapper {
    /** 更新已有标识的标签，不改变业务引用键。Update the label without changing the business reference key. */
    @Update("UPDATE xnet_mlops_smp_dataset_config_info SET label = #{label} WHERE config_type = #{configType} AND value = #{value}")
    int updateDatasetConfigLabel(@Param("label") String label, @Param("value") String value, @Param("configType") String configType);

    /** 检查其他配置项的标签冲突。Check label conflicts with other configuration items. */
    @Select("SELECT COUNT(*) FROM xnet_mlops_smp_dataset_config_info WHERE label = #{label} AND NOT (config_type = #{configType} AND value = #{value})")
    int countOtherLabels(@Param("label") String label, @Param("value") String value, @Param("configType") String configType);

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
