package com.synapxnet.mlopsmtpservice.service;

import com.synapxnet.mlopsmtpservice.entity.Algorithm;
import com.synapxnet.mlopsmtpservice.entity.AlgorithmRepository;
import com.synapxnet.mlopsmtpservice.mapper.AlgorithmMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlgorithmServiceImpl implements AlgorithmService {

    private final AlgorithmMapper algorithmMapper;

    @Autowired
    public AlgorithmServiceImpl(AlgorithmMapper algorithmMapper) {
        this.algorithmMapper = algorithmMapper;
    }

    // 内部使用的名称唯一性检查方法
    private boolean existsByAlgorithmName(String algorithmName) {
        return algorithmMapper.countByAlgorithmName(algorithmName) > 0;
    }

    @Override
    @Transactional
    public Algorithm createAlgorithm(Algorithm algorithm) {
        // 验证算法名称
        if (algorithm.getAlgorithm_name() == null || algorithm.getAlgorithm_name().isEmpty()) {
            throw new RuntimeException("算法名称不能为空");
        }

        String algorithmName = algorithm.getAlgorithm_name().trim();

        // 检查算法名称唯一性
        if (existsByAlgorithmName(algorithmName)) {
            throw new RuntimeException("算法名称已存在: " + algorithmName);
        }

        // 设置唯一标识
        algorithm.setUid("ALG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        algorithm.setAlgorithm_name(algorithmName);

        // 设置默认值
        if (algorithm.getVersion() == null || algorithm.getVersion().isEmpty()) {
            algorithm.setVersion("1.0.0");
        }

        algorithmMapper.insertAlgorithm(algorithm);
        return algorithmMapper.selectById(algorithm.getId());
    }

    @Override
    public List<Algorithm> getAllAlgorithms() {
        return algorithmMapper.findAll();
    }

    @Override
    public Algorithm getAlgorithmById(Long id) {
        return algorithmMapper.findById(id)
                .orElseThrow(() -> new RuntimeException("算法不存在: " + id));
    }

    @Override
    public Algorithm getAlgorithmByUID(String UID) {
        return algorithmMapper.findAlgorithmsByUID(UID)
                .orElseThrow(() -> new RuntimeException("算法不存在: " + UID));
    }

    @Override
    @Transactional
    public Algorithm updateAlgorithm(Long id, Algorithm algorithm) {
        // 检查算法是否存在
        Algorithm existingAlgorithm = getAlgorithmById(id);

        // 验证算法名称
        if (algorithm.getAlgorithm_name() == null || algorithm.getAlgorithm_name().isEmpty()) {
            throw new RuntimeException("算法名称不能为空");
        }

        String newAlgorithmName = algorithm.getAlgorithm_name().trim();
        algorithm.setAlgorithm_name(newAlgorithmName);

        // 获取现有算法名称
        String existingName = existingAlgorithm.getAlgorithm_name();
        if (existingName != null) {
            existingName = existingName.trim();
        }

        
        // 如果名称变更且新名称已存在
        if (existingName == null || !existingName.equals(newAlgorithmName)) {
            if (existsByAlgorithmName(newAlgorithmName)) {
                throw new RuntimeException("算法名称已存在: " + newAlgorithmName);
            }
        }

        algorithm.setId(id);
        algorithmMapper.updateAlgorithm(algorithm);
        return algorithmMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteAlgorithm(Long id) {
        // 检查算法是否存在
        getAlgorithmById(id); // 如果不存在会抛出异常

        algorithmMapper.deleteById(id);
    }

    @Override
    public AlgorithmRepository getAlgorithmByUid(String algorithmUid) {
        Optional<AlgorithmRepository> algorithm = algorithmMapper.findByUid(algorithmUid);
        return algorithm.orElse(null); // 正确解包Optional
    }

}