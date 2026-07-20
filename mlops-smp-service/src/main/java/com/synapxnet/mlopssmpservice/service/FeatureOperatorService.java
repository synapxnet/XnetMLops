package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.FeatureOperator;
import java.util.List;

public interface FeatureOperatorService {
    FeatureOperator createOperator(FeatureOperator featureOperator);
    FeatureOperator updateOperator(Long id, FeatureOperator featureOperator);
    void deleteOperator(Long id);
    FeatureOperator getOperatorById(Long id);
    List<FeatureOperator> getAllOperators();
    List<FeatureOperator> searchOperators(String tenantUid, String deptUid, String operatorName);
    FeatureOperator getOperatorByUid(String uid);
    List<FeatureOperator> searchOperators(String tenantUid, String deptUid, String teamUid, String operatorName, String operatorCode, String version);
    List<FeatureOperator> getOperatorsByCode(String operatorCode);
}
