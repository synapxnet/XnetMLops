package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import com.synapxnet.mlopsdppservice.mapper.DatasetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class DatasetServiceImpl implements DatasetService {

    private final DatasetMapper datasetMapper;

    @Autowired
    public DatasetServiceImpl(DatasetMapper datasetMapper) {
        this.datasetMapper = datasetMapper;
    }

    @Override
    public boolean existsByDatasetFile(String datasetFile) {
        return datasetMapper.countByDatasetFile(datasetFile) > 0;
    }

    @Override
    @Transactional
    public Dataset createDataset(Dataset dataset) {
        // 添加空值检查
        if (dataset.getDataset_file() == null || dataset.getDataset_file().isEmpty()) {
            throw new RuntimeException("数据集名称不能为空");
        }

        // 修复：添加 trim() 处理
        String datasetFile = dataset.getDataset_file().trim();

        // 校验数据集名称唯一性
        if (existsByDatasetFile(datasetFile)) {
            throw new RuntimeException("数据集名称已存在: " + datasetFile);
        }

        // 确保设置正确的名称
        dataset.setDataset_file(datasetFile);

        dataset.setUid("DS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        Date now = new Date();
        dataset.setCreated_at(now);
        dataset.setUpdated_at(now);

        datasetMapper.insertDataset(dataset);
        return datasetMapper.selectById(dataset.getId());
    }

    @Override
    public List<Dataset> getAllDatasets() {
        return datasetMapper.findAll();
    }

    @Override
    public Optional<Dataset> findDatasetById(Long id) {
        Dataset dataset = datasetMapper.findById(id);
        return Optional.ofNullable(dataset);
    }

    @Override
    public Dataset getDatasetById(Long id) {
        Dataset dataset = datasetMapper.findById(id);
        if (dataset == null) {
            throw new RuntimeException("数据集不存在: " + id);
        }
        return dataset;
    }

    @Override
    public boolean deleteDataset(Long id) {
        Dataset dataset = datasetMapper.findById(id);
        if (dataset == null) {
            return false;
        }
        return datasetMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public Dataset updateDataset(Long id, Dataset dataset) {
        ensureDatasetExists(id);

        // 添加空值检查
        if (dataset.getDataset_file() == null || dataset.getDataset_file().isEmpty()) {
            throw new RuntimeException("数据集名称不能为空");
        }

        // 修复：添加 trim() 处理
        String newDatasetFile = dataset.getDataset_file().trim();
        dataset.setDataset_file(newDatasetFile);

        // 获取当前数据集
        Dataset existingDataset = getDatasetById(id);

        // 修复：使用 equals() 而不是 == 比较字符串
        // 修复：添加 null 检查
        String existingFile = existingDataset.getDataset_file();
        if (existingFile != null) {
            existingFile = existingFile.trim();
        }

        // 检查名称是否变更且新名称是否已存在
        if (existingFile == null || !existingFile.equals(newDatasetFile)) {
            if (existsByDatasetFile(newDatasetFile)) {
                throw new RuntimeException("数据集名称已存在: " + newDatasetFile);
            }
        }

        dataset.setId(id);
        dataset.setUpdated_at(new Date());
        datasetMapper.updateDataset(dataset);
        return datasetMapper.selectById(id);
    }

    private void ensureDatasetExists(Long id) {
        Dataset dataset = datasetMapper.findById(id);
        if (dataset == null) {
            throw new RuntimeException("数据集不存在: " + id);
        }
    }
}
