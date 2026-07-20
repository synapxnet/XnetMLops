package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.AlgorithmRepository;
import com.synapxnet.mlopssmpservice.exception.DuplicateEntryException;
import com.synapxnet.mlopssmpservice.exception.EntityNotFoundException;
import com.synapxnet.mlopssmpservice.mapper.AlgorithmRepositoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AlgorithmRepositoryServiceImpl implements AlgorithmRepositoryService {

    private final AlgorithmRepositoryMapper algorithmRepositoryMapper;

    @Autowired
    public AlgorithmRepositoryServiceImpl(AlgorithmRepositoryMapper algorithmRepositoryMapper) {
        this.algorithmRepositoryMapper = algorithmRepositoryMapper;
    }

    @Override
    public AlgorithmRepository createAlgorithm(AlgorithmRepository algorithmRepository) {
        checkForDuplicate(algorithmRepository, null);
        // 设置系统字段
        algorithmRepository.setUid(UUID.randomUUID().toString());
        algorithmRepository.setCreated_at(new Date());
        algorithmRepository.setUpdated_at(new Date());

        // 插入数据库
        algorithmRepositoryMapper.insert(algorithmRepository);

        // 返回包含完整信息的算法对象
        return algorithmRepositoryMapper.findById(algorithmRepository.getId())
                .orElseThrow(() -> new EntityNotFoundException("Algorithm creation failed"));
    }

    @Override
    public AlgorithmRepository updateAlgorithm(Long id, AlgorithmRepository algorithmRepository) {
        checkForDuplicate(algorithmRepository, id);
        // 验证算法是否存在
        AlgorithmRepository existing = algorithmRepositoryMapper.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Algorithm not found with id: " + id));

        // 更新允许修改的字段
        existing.setUrl(algorithmRepository.getUrl());
        existing.setAlgorithm(algorithmRepository.getAlgorithm());
        existing.setAlgorithm_version(algorithmRepository.getAlgorithm_version());
        existing.setDescription(algorithmRepository.getDescription());
        existing.setAuthorized_tenants(algorithmRepository.getAuthorized_tenants());
        existing.setUpdated_at(new Date());
        existing.setUpdated_by(algorithmRepository.getUpdated_by());

        // 如果需要更新加密令牌
        if (algorithmRepository.getEncrypted_token() != null) {
            existing.setEncrypted_token(algorithmRepository.getEncrypted_token());
        }

        algorithmRepositoryMapper.update(existing);
        return algorithmRepositoryMapper.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Algorithm update failed"));
    }

    @Override
    public void deleteAlgorithm(Long id) {
        // 验证算法是否存在
        if (algorithmRepositoryMapper.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Algorithm not found with id: " + id);
        }
        algorithmRepositoryMapper.deleteById(id);
    }

    @Override
    public AlgorithmRepository getAlgorithmById(Long id) {
        return algorithmRepositoryMapper.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Algorithm not found with id: " + id));
    }

    @Override
    public List<AlgorithmRepository> getAllAlgorithms() {
        return algorithmRepositoryMapper.selectAllWithOrgInfo();
    }

    @Override
    public List<AlgorithmRepository> searchAlgorithms(String tenantUid, String deptUid, String algorithmName) {
        return algorithmRepositoryMapper.searchAlgorithms(
                tenantUid,
                deptUid,
                null,       // teamUid
                algorithmName,
                null        // version
        );
    }

    @Override
    public AlgorithmRepository getAlgorithmByUid(String uid) {
        return algorithmRepositoryMapper.findByUid(uid)
                .orElseThrow(() -> new EntityNotFoundException("Algorithm not found with uid: " + uid));
    }

    @Override
    public List<AlgorithmRepository> searchAlgorithms(
            String tenantUid,
            String deptUid,
            String teamUid,
            String algorithm,
            String version) {
        return algorithmRepositoryMapper.searchAlgorithms(
                tenantUid, deptUid, teamUid, algorithm, version
        );
    }
    private void checkForDuplicate(AlgorithmRepository algorithmRepository, Long excludeId) {
        int count = algorithmRepositoryMapper.countByUrlAndNameAndVersion(
                algorithmRepository.getUrl(),
                algorithmRepository.getAlgorithm(),
                algorithmRepository.getAlgorithm_version(),
                excludeId
        );
        if (count > 0) {
            throw new DuplicateEntryException("相同URL、算法名称和版本已存在");
        }
    }
}