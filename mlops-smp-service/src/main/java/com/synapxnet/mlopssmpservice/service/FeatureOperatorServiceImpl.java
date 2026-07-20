package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import com.synapxnet.mlopssmpservice.exception.DuplicateEntryException;
import com.synapxnet.mlopssmpservice.exception.EntityNotFoundException;
import com.synapxnet.mlopssmpservice.mapper.FeatureOperatorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FeatureOperatorServiceImpl implements FeatureOperatorService {

    private final FeatureOperatorMapper featureOperatorMapper;

    @Autowired
    public FeatureOperatorServiceImpl(FeatureOperatorMapper featureOperatorMapper) {
        this.featureOperatorMapper = featureOperatorMapper;
    }

    @Override
    public FeatureOperator createOperator(FeatureOperator featureOperator) {
        checkForDuplicate(featureOperator, null);
        // 设置系统字段
        featureOperator.setUid(UUID.randomUUID().toString());
        featureOperator.setCreated_at(new Date());
        featureOperator.setUpdated_at(new Date());

        // 插入数据库
        featureOperatorMapper.insert(featureOperator);

        // 返回包含完整信息的对象
        return featureOperatorMapper.findById(featureOperator.getId())
                .orElseThrow(() -> new EntityNotFoundException("Feature operator creation failed"));
    }

    @Override
    public FeatureOperator updateOperator(Long id, FeatureOperator featureOperator) {
        checkForDuplicate(featureOperator, id);
        // 验证是否存在
        FeatureOperator existing = featureOperatorMapper.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature operator not found with id: " + id));

        // 更新允许修改的字段
        existing.setUrl(featureOperator.getUrl());
        existing.setOperator_name(featureOperator.getOperator_name());
        existing.setOperator_code(featureOperator.getOperator_code());
        existing.setOperator_version(featureOperator.getOperator_version());
        existing.setDescription(featureOperator.getDescription());
        existing.setAuthorized_tenants(featureOperator.getAuthorized_tenants());
        existing.setUpdated_at(new Date());
        existing.setUpdated_by(featureOperator.getUpdated_by());

        // 如果需要更新加密令牌
        if (featureOperator.getEncrypted_token() != null) {
            existing.setEncrypted_token(featureOperator.getEncrypted_token());
        }

        featureOperatorMapper.update(existing);
        return featureOperatorMapper.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature operator update failed"));
    }

    @Override
    public void deleteOperator(Long id) {
        // 验证是否存在
        if (featureOperatorMapper.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Feature operator not found with id: " + id);
        }
        featureOperatorMapper.deleteById(id);
    }

    @Override
    public FeatureOperator getOperatorById(Long id) {
        return featureOperatorMapper.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature operator not found with id: " + id));
    }

    @Override
    public List<FeatureOperator> getAllOperators() {
        return featureOperatorMapper.selectAllWithOrgInfo();
    }

    @Override
    public List<FeatureOperator> searchOperators(String tenantUid, String deptUid, String operatorName) {
        return featureOperatorMapper.searchOperators(
                tenantUid,
                deptUid,
                null,       // teamUid
                operatorName,
                null,       // operatorCode
                null        // version
        );
    }

    @Override
    public FeatureOperator getOperatorByUid(String uid) {
        return featureOperatorMapper.findByUid(uid)
                .orElseThrow(() -> new EntityNotFoundException("Feature operator not found with uid: " + uid));
    }

    @Override
    public List<FeatureOperator> searchOperators(
            String tenantUid,
            String deptUid,
            String teamUid,
            String operatorName,
            String operatorCode,
            String version) {
        return featureOperatorMapper.searchOperators(
                tenantUid, deptUid, teamUid, operatorName, operatorCode, version
        );
    }

    @Override
    public List<FeatureOperator> getOperatorsByCode(String operatorCode) {
        return featureOperatorMapper.findByOperatorCode(operatorCode);
    }

    private void checkForDuplicate(FeatureOperator featureOperator, Long excludeId) {
        int count = featureOperatorMapper.countByUrlAndCodeAndVersion(
                featureOperator.getUrl(),
                featureOperator.getOperator_code(),
                featureOperator.getOperator_version(),
                excludeId
        );
        if (count > 0) {
            throw new DuplicateEntryException("相同URL、算子代码和版本已存在");
        }
    }
}
