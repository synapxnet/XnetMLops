package com.synapxnet.mlopsdppservice.mapper;

import com.synapxnet.mlopsdppservice.entity.FeatureOperator;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FeatureOperatorMapper {

    @Results(id = "featureOperatorResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "uid", column = "uid"),
            @Result(property = "name", column = "name"),
            @Result(property = "code", column = "code"),
            @Result(property = "description", column = "description"),
            @Result(property = "category", column = "category"),
            @Result(property = "outputFormats", column = "output_formats"),
            @Result(property = "featureColumns", column = "feature_columns"),
            @Result(property = "parameterSchema", column = "parameter_schema"),
            @Result(property = "sortOrder", column = "sort_order"),
            @Result(property = "enabled", column = "enabled"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT * FROM xnet_mlops_dpp_feature_operator ORDER BY sort_order ASC, created_at DESC")
    List<FeatureOperator> findAll();

    @ResultMap("featureOperatorResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_operator WHERE enabled = true ORDER BY sort_order ASC")
    List<FeatureOperator> findAllEnabled();

    @ResultMap("featureOperatorResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_operator WHERE id = #{id}")
    FeatureOperator findById(Long id);

    @ResultMap("featureOperatorResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_operator WHERE uid = #{uid}")
    FeatureOperator findByUid(String uid);

    @ResultMap("featureOperatorResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_operator WHERE code = #{code}")
    FeatureOperator findByCode(String code);

    @ResultMap("featureOperatorResultMap")
    @Select("SELECT * FROM xnet_mlops_dpp_feature_operator WHERE category = #{category} AND enabled = true ORDER BY sort_order ASC")
    List<FeatureOperator> findByCategory(String category);

    @Insert("INSERT INTO xnet_mlops_dpp_feature_operator (" +
            "uid, name, code, description, category, output_formats, feature_columns, " +
            "parameter_schema, sort_order, enabled, created_by, created_at, updated_at" +
            ") VALUES (" +
            "#{uid}, #{name}, #{code}, #{description}, #{category}, #{outputFormats}, #{featureColumns}, " +
            "#{parameterSchema}, #{sortOrder}, #{enabled}, #{createdBy}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeatureOperator operator);

    @Update("UPDATE xnet_mlops_dpp_feature_operator SET " +
            "name = #{name}, " +
            "code = #{code}, " +
            "description = #{description}, " +
            "category = #{category}, " +
            "output_formats = #{outputFormats}, " +
            "feature_columns = #{featureColumns}, " +
            "parameter_schema = #{parameterSchema}, " +
            "sort_order = #{sortOrder}, " +
            "enabled = #{enabled}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(FeatureOperator operator);

    @Update("UPDATE xnet_mlops_dpp_feature_operator SET enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    @Delete("DELETE FROM xnet_mlops_dpp_feature_operator WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_feature_operator WHERE code = #{code}")
    int countByCode(String code);

    @Select("SELECT COUNT(*) FROM xnet_mlops_dpp_feature_operator WHERE code = #{code} AND id != #{id}")
    int countByCodeExcludeId(@Param("code") String code, @Param("id") Long id);
}
