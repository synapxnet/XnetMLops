package com.synapxnet.mlopsdppservice.controller;

import com.synapxnet.mlopsdppservice.entity.FeatureOperator;
import com.synapxnet.mlopsdppservice.mapper.FeatureOperatorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 特征算子管理Controller
 */
@RestController
@RequestMapping("/api/dpp/feature-operators")
@CrossOrigin(origins = "*")
public class FeatureOperatorController {

    @Autowired
    private FeatureOperatorMapper featureOperatorMapper;

    /**
     * 获取所有启用的特征算子
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOperators() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<FeatureOperator> operators = featureOperatorMapper.findAllEnabled();
            response.put("code", 0);
            response.put("message", "success");
            response.put("data", operators);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取特征算子列表失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有特征算子（包括禁用的，管理用）
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllOperatorsIncludeDisabled() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<FeatureOperator> operators = featureOperatorMapper.findAll();
            response.put("code", 0);
            response.put("message", "success");
            response.put("data", operators);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取特征算子列表失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 根据类别获取特征算子
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getOperatorsByCategory(@PathVariable("category") String category) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<FeatureOperator> operators = featureOperatorMapper.findByCategory(category);
            response.put("code", 0);
            response.put("message", "success");
            response.put("data", operators);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取特征算子失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取特征算子
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOperatorById(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            FeatureOperator operator = featureOperatorMapper.findById(id);
            if (operator != null) {
                response.put("code", 0);
                response.put("message", "success");
                response.put("data", operator);
            } else {
                response.put("code", 404);
                response.put("message", "特征算子不存在");
                response.put("data", null);
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取特征算子失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 根据代码获取特征算子
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Map<String, Object>> getOperatorByCode(@PathVariable("code") String code) {
        Map<String, Object> response = new HashMap<>();
        try {
            FeatureOperator operator = featureOperatorMapper.findByCode(code);
            if (operator != null) {
                response.put("code", 0);
                response.put("message", "success");
                response.put("data", operator);
            } else {
                response.put("code", 404);
                response.put("message", "特征算子不存在");
                response.put("data", null);
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取特征算子失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 创建特征算子
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOperator(@RequestBody FeatureOperator operator) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 验证code唯一性
            if (featureOperatorMapper.countByCode(operator.getCode()) > 0) {
                response.put("code", 400);
                response.put("message", "算子代码已存在");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            operator.setUid(UUID.randomUUID().toString());
            if (operator.getEnabled() == null) {
                operator.setEnabled(true);
            }
            if (operator.getSortOrder() == null) {
                operator.setSortOrder(0);
            }

            featureOperatorMapper.insert(operator);

            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", operator);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "创建特征算子失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 更新特征算子
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateOperator(
            @PathVariable("id") Long id,
            @RequestBody FeatureOperator operator) {
        Map<String, Object> response = new HashMap<>();
        try {
            FeatureOperator existing = featureOperatorMapper.findById(id);
            if (existing == null) {
                response.put("code", 404);
                response.put("message", "特征算子不存在");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            // 验证code唯一性（排除自己）
            if (featureOperatorMapper.countByCodeExcludeId(operator.getCode(), id) > 0) {
                response.put("code", 400);
                response.put("message", "算子代码已存在");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            operator.setId(id);
            operator.setUid(existing.getUid());
            featureOperatorMapper.update(operator);

            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", operator);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "更新特征算子失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 启用/禁用特征算子
     */
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<Map<String, Object>> toggleEnabled(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Boolean> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Boolean enabled = body.get("enabled");
            if (enabled == null) {
                response.put("code", 400);
                response.put("message", "参数enabled不能为空");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            featureOperatorMapper.updateEnabled(id, enabled);

            response.put("code", 0);
            response.put("message", enabled ? "启用成功" : "禁用成功");
            response.put("data", null);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "操作失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 删除特征算子
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteOperator(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            FeatureOperator existing = featureOperatorMapper.findById(id);
            if (existing == null) {
                response.put("code", 404);
                response.put("message", "特征算子不存在");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            featureOperatorMapper.deleteById(id);

            response.put("code", 0);
            response.put("message", "删除成功");
            response.put("data", null);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "删除特征算子失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 初始化默认特征算子（用于系统首次部署）
     */
    @PostMapping("/init-defaults")
    public ResponseEntity<Map<String, Object>> initDefaultOperators() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<FeatureOperator> created = new ArrayList<>();

            // 1. CSV转换算子
            if (featureOperatorMapper.countByCode("to_csv") == 0) {
                FeatureOperator csvOperator = new FeatureOperator();
                csvOperator.setUid(UUID.randomUUID().toString());
                csvOperator.setName("CSV转换");
                csvOperator.setCode("to_csv");
                csvOperator.setDescription("将数据表转换为CSV格式文件，支持特征类型转换和默认值填充");
                csvOperator.setCategory("format_conversion");
                csvOperator.setOutputFormats("[\"csv\"]");
                csvOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"特征类型\",\"type\":\"select\",\"options\":[{\"label\":\"整数(int)\",\"value\":\"int\"},{\"label\":\"浮点数(float)\",\"value\":\"float\"},{\"label\":\"字符串(string)\",\"value\":\"string\"},{\"label\":\"布尔(bool)\",\"value\":\"bool\"}],\"default\":\"string\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"}]");
                csvOperator.setParameterSchema("{\"delimiter\":{\"type\":\"select\",\"label\":\"分隔符\",\"options\":[\",\",\";\",\"\\\\t\",\"|\"],\"default\":\",\"},\"encoding\":{\"type\":\"select\",\"label\":\"编码\",\"options\":[\"utf-8\",\"gbk\",\"gb2312\"],\"default\":\"utf-8\"},\"includeHeader\":{\"type\":\"switch\",\"label\":\"包含表头\",\"default\":true}}");
                csvOperator.setSortOrder(1);
                csvOperator.setEnabled(true);
                csvOperator.setCreatedBy("system");
                featureOperatorMapper.insert(csvOperator);
                created.add(csvOperator);
            }

            // 2. Parquet转换算子
            if (featureOperatorMapper.countByCode("to_parquet") == 0) {
                FeatureOperator parquetOperator = new FeatureOperator();
                parquetOperator.setUid(UUID.randomUUID().toString());
                parquetOperator.setName("Parquet转换");
                parquetOperator.setCode("to_parquet");
                parquetOperator.setDescription("将数据表转换为Parquet列式存储格式，适合大数据分析场景");
                parquetOperator.setCategory("format_conversion");
                parquetOperator.setOutputFormats("[\"parquet\"]");
                parquetOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"特征类型\",\"type\":\"select\",\"options\":[{\"label\":\"整数(int64)\",\"value\":\"int64\"},{\"label\":\"浮点数(double)\",\"value\":\"double\"},{\"label\":\"字符串(string)\",\"value\":\"string\"},{\"label\":\"布尔(bool)\",\"value\":\"bool\"},{\"label\":\"列表(list)\",\"value\":\"list\"}],\"default\":\"string\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"},{\"key\":\"nullable\",\"title\":\"允许空值\",\"type\":\"switch\",\"default\":true}]");
                parquetOperator.setParameterSchema("{\"compression\":{\"type\":\"select\",\"label\":\"压缩算法\",\"options\":[\"snappy\",\"gzip\",\"lz4\",\"zstd\",\"none\"],\"default\":\"snappy\"},\"rowGroupSize\":{\"type\":\"number\",\"label\":\"行组大小\",\"default\":100000,\"min\":1000,\"max\":10000000}}");
                parquetOperator.setSortOrder(2);
                parquetOperator.setEnabled(true);
                parquetOperator.setCreatedBy("system");
                featureOperatorMapper.insert(parquetOperator);
                created.add(parquetOperator);
            }

            // 3. TFRecord转换算子
            if (featureOperatorMapper.countByCode("to_tfrecord") == 0) {
                FeatureOperator tfrecordOperator = new FeatureOperator();
                tfrecordOperator.setUid(UUID.randomUUID().toString());
                tfrecordOperator.setName("TFRecord转换");
                tfrecordOperator.setCode("to_tfrecord");
                tfrecordOperator.setDescription("将数据表转换为TensorFlow的TFRecord格式，适合深度学习训练");
                tfrecordOperator.setCategory("format_conversion");
                tfrecordOperator.setOutputFormats("[\"tfrecord\"]");
                tfrecordOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"TF特征类型\",\"type\":\"select\",\"options\":[{\"label\":\"Int64List\",\"value\":\"int64_list\"},{\"label\":\"FloatList\",\"value\":\"float_list\"},{\"label\":\"BytesList\",\"value\":\"bytes_list\"}],\"default\":\"bytes_list\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"},{\"key\":\"isSequence\",\"title\":\"序列特征\",\"type\":\"switch\",\"default\":false}]");
                tfrecordOperator.setParameterSchema("{\"shardCount\":{\"type\":\"number\",\"label\":\"分片数量\",\"default\":1,\"min\":1,\"max\":1000},\"compression\":{\"type\":\"select\",\"label\":\"压缩类型\",\"options\":[\"GZIP\",\"ZLIB\",\"none\"],\"default\":\"GZIP\"}}");
                tfrecordOperator.setSortOrder(3);
                tfrecordOperator.setEnabled(true);
                tfrecordOperator.setCreatedBy("system");
                featureOperatorMapper.insert(tfrecordOperator);
                created.add(tfrecordOperator);
            }

            // 4. TXT转换算子
            if (featureOperatorMapper.countByCode("to_txt") == 0) {
                FeatureOperator txtOperator = new FeatureOperator();
                txtOperator.setUid(UUID.randomUUID().toString());
                txtOperator.setName("TXT转换");
                txtOperator.setCode("to_txt");
                txtOperator.setDescription("将数据表转换为纯文本格式，支持自定义分隔符和格式");
                txtOperator.setCategory("format_conversion");
                txtOperator.setOutputFormats("[\"txt\"]");
                txtOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"特征类型\",\"type\":\"select\",\"options\":[{\"label\":\"整数(int)\",\"value\":\"int\"},{\"label\":\"浮点数(float)\",\"value\":\"float\"},{\"label\":\"字符串(string)\",\"value\":\"string\"}],\"default\":\"string\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"}]");
                txtOperator.setParameterSchema("{\"delimiter\":{\"type\":\"select\",\"label\":\"分隔符\",\"options\":[\"\\\\t\",\" \",\",\",\"|\"],\"default\":\"\\\\t\"},\"lineEnding\":{\"type\":\"select\",\"label\":\"换行符\",\"options\":[\"\\\\n\",\"\\\\r\\\\n\"],\"default\":\"\\\\n\"},\"encoding\":{\"type\":\"select\",\"label\":\"编码\",\"options\":[\"utf-8\",\"gbk\",\"gb2312\"],\"default\":\"utf-8\"}}");
                txtOperator.setSortOrder(4);
                txtOperator.setEnabled(true);
                txtOperator.setCreatedBy("system");
                featureOperatorMapper.insert(txtOperator);
                created.add(txtOperator);
            }

            // 5. JSON转换算子
            if (featureOperatorMapper.countByCode("to_json") == 0) {
                FeatureOperator jsonOperator = new FeatureOperator();
                jsonOperator.setUid(UUID.randomUUID().toString());
                jsonOperator.setName("JSON转换");
                jsonOperator.setCode("to_json");
                jsonOperator.setDescription("将数据表转换为JSON格式，支持单行或多行JSON");
                jsonOperator.setCategory("format_conversion");
                jsonOperator.setOutputFormats("[\"json\",\"jsonl\"]");
                jsonOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"特征类型\",\"type\":\"select\",\"options\":[{\"label\":\"整数(int)\",\"value\":\"int\"},{\"label\":\"浮点数(float)\",\"value\":\"float\"},{\"label\":\"字符串(string)\",\"value\":\"string\"},{\"label\":\"布尔(bool)\",\"value\":\"bool\"},{\"label\":\"数组(array)\",\"value\":\"array\"},{\"label\":\"对象(object)\",\"value\":\"object\"}],\"default\":\"string\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"},{\"key\":\"jsonPath\",\"title\":\"JSON路径\",\"type\":\"input\",\"placeholder\":\"嵌套路径，如: data.items\"}]");
                jsonOperator.setParameterSchema("{\"format\":{\"type\":\"select\",\"label\":\"输出格式\",\"options\":[{\"label\":\"JSON数组\",\"value\":\"array\"},{\"label\":\"JSON Lines\",\"value\":\"jsonl\"}],\"default\":\"array\"},\"pretty\":{\"type\":\"switch\",\"label\":\"格式化输出\",\"default\":false},\"encoding\":{\"type\":\"select\",\"label\":\"编码\",\"options\":[\"utf-8\"],\"default\":\"utf-8\"}}");
                jsonOperator.setSortOrder(5);
                jsonOperator.setEnabled(true);
                jsonOperator.setCreatedBy("system");
                featureOperatorMapper.insert(jsonOperator);
                created.add(jsonOperator);
            }

            // 6. 特征标准化算子
            if (featureOperatorMapper.countByCode("normalize") == 0) {
                FeatureOperator normalizeOperator = new FeatureOperator();
                normalizeOperator.setUid(UUID.randomUUID().toString());
                normalizeOperator.setName("特征标准化");
                normalizeOperator.setCode("normalize");
                normalizeOperator.setDescription("对数值特征进行标准化处理（Z-Score或Min-Max）");
                normalizeOperator.setCategory("feature_transform");
                normalizeOperator.setOutputFormats("[\"csv\",\"parquet\",\"json\"]");
                normalizeOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"特征类型\",\"type\":\"select\",\"options\":[{\"label\":\"数值(参与标准化)\",\"value\":\"numeric\"},{\"label\":\"类别(不参与)\",\"value\":\"categorical\"},{\"label\":\"跳过\",\"value\":\"skip\"}],\"default\":\"numeric\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"},{\"key\":\"normalizeMethod\",\"title\":\"标准化方法\",\"type\":\"select\",\"options\":[{\"label\":\"Z-Score\",\"value\":\"zscore\"},{\"label\":\"Min-Max\",\"value\":\"minmax\"},{\"label\":\"不处理\",\"value\":\"none\"}],\"default\":\"zscore\"}]");
                normalizeOperator.setParameterSchema("{\"outputFormat\":{\"type\":\"select\",\"label\":\"输出格式\",\"options\":[\"csv\",\"parquet\",\"json\"],\"default\":\"csv\"},\"saveStats\":{\"type\":\"switch\",\"label\":\"保存统计信息\",\"default\":true}}");
                normalizeOperator.setSortOrder(10);
                normalizeOperator.setEnabled(true);
                normalizeOperator.setCreatedBy("system");
                featureOperatorMapper.insert(normalizeOperator);
                created.add(normalizeOperator);
            }

            // 7. 独热编码算子
            if (featureOperatorMapper.countByCode("one_hot_encode") == 0) {
                FeatureOperator oneHotOperator = new FeatureOperator();
                oneHotOperator.setUid(UUID.randomUUID().toString());
                oneHotOperator.setName("独热编码");
                oneHotOperator.setCode("one_hot_encode");
                oneHotOperator.setDescription("将类别特征转换为独热编码向量");
                oneHotOperator.setCategory("feature_transform");
                oneHotOperator.setOutputFormats("[\"csv\",\"parquet\",\"tfrecord\"]");
                oneHotOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"编码类型\",\"type\":\"select\",\"options\":[{\"label\":\"独热编码\",\"value\":\"onehot\"},{\"label\":\"标签编码\",\"value\":\"label\"},{\"label\":\"保持原样\",\"value\":\"keep\"}],\"default\":\"keep\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"},{\"key\":\"maxCategories\",\"title\":\"最大类别数\",\"type\":\"number\",\"default\":100,\"placeholder\":\"超过则使用其他类别\"}]");
                oneHotOperator.setParameterSchema("{\"outputFormat\":{\"type\":\"select\",\"label\":\"输出格式\",\"options\":[\"csv\",\"parquet\",\"tfrecord\"],\"default\":\"csv\"},\"handleUnknown\":{\"type\":\"select\",\"label\":\"未知类别处理\",\"options\":[{\"label\":\"报错\",\"value\":\"error\"},{\"label\":\"忽略\",\"value\":\"ignore\"},{\"label\":\"设为其他\",\"value\":\"other\"}],\"default\":\"ignore\"}}");
                oneHotOperator.setSortOrder(11);
                oneHotOperator.setEnabled(true);
                oneHotOperator.setCreatedBy("system");
                featureOperatorMapper.insert(oneHotOperator);
                created.add(oneHotOperator);
            }

            // 8. 数据清洗算子
            if (featureOperatorMapper.countByCode("data_cleaning") == 0) {
                FeatureOperator cleaningOperator = new FeatureOperator();
                cleaningOperator.setUid(UUID.randomUUID().toString());
                cleaningOperator.setName("数据清洗");
                cleaningOperator.setCode("data_cleaning");
                cleaningOperator.setDescription("处理缺失值、异常值和重复数据");
                cleaningOperator.setCategory("data_cleaning");
                cleaningOperator.setOutputFormats("[\"csv\",\"parquet\",\"json\"]");
                cleaningOperator.setFeatureColumns("[{\"key\":\"featureType\",\"title\":\"数据类型\",\"type\":\"select\",\"options\":[{\"label\":\"数值型\",\"value\":\"numeric\"},{\"label\":\"字符串\",\"value\":\"string\"},{\"label\":\"日期时间\",\"value\":\"datetime\"}],\"default\":\"string\"},{\"key\":\"defaultValue\",\"title\":\"默认值\",\"type\":\"input\",\"placeholder\":\"为空/null时的填充值\"},{\"key\":\"fillStrategy\",\"title\":\"填充策略\",\"type\":\"select\",\"options\":[{\"label\":\"使用默认值\",\"value\":\"default\"},{\"label\":\"使用均值\",\"value\":\"mean\"},{\"label\":\"使用中位数\",\"value\":\"median\"},{\"label\":\"使用众数\",\"value\":\"mode\"},{\"label\":\"删除行\",\"value\":\"drop\"}],\"default\":\"default\"},{\"key\":\"outlierMethod\",\"title\":\"异常值处理\",\"type\":\"select\",\"options\":[{\"label\":\"不处理\",\"value\":\"none\"},{\"label\":\"删除\",\"value\":\"remove\"},{\"label\":\"截断(3σ)\",\"value\":\"clip\"}],\"default\":\"none\"}]");
                cleaningOperator.setParameterSchema("{\"outputFormat\":{\"type\":\"select\",\"label\":\"输出格式\",\"options\":[\"csv\",\"parquet\",\"json\"],\"default\":\"csv\"},\"removeDuplicates\":{\"type\":\"switch\",\"label\":\"去除重复行\",\"default\":false},\"trimWhitespace\":{\"type\":\"switch\",\"label\":\"去除首尾空格\",\"default\":true}}");
                cleaningOperator.setSortOrder(20);
                cleaningOperator.setEnabled(true);
                cleaningOperator.setCreatedBy("system");
                featureOperatorMapper.insert(cleaningOperator);
                created.add(cleaningOperator);
            }

            response.put("code", 0);
            response.put("message", "初始化完成，创建了 " + created.size() + " 个默认算子");
            response.put("data", created);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "初始化失败: " + e.getMessage());
            response.put("data", null);
        }
        return ResponseEntity.ok(response);
    }
}
