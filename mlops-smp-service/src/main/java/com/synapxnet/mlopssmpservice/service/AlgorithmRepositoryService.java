package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.AlgorithmRepository;
import java.util.List;

public interface AlgorithmRepositoryService {
    AlgorithmRepository createAlgorithm(AlgorithmRepository algorithmRepository);
    AlgorithmRepository updateAlgorithm(Long id, AlgorithmRepository algorithmRepository);
    void deleteAlgorithm(Long id);
    AlgorithmRepository getAlgorithmById(Long id);
    List<AlgorithmRepository> getAllAlgorithms();
    List<AlgorithmRepository> searchAlgorithms(String tenantUid, String deptUid, String algorithmName);
    AlgorithmRepository getAlgorithmByUid(String uid);
    List<AlgorithmRepository> searchAlgorithms(String tenantUid, String deptUid, String teamUid, String algorithm, String version);
}