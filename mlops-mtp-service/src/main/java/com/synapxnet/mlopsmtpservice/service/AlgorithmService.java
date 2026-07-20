package com.synapxnet.mlopsmtpservice.service;

import com.synapxnet.mlopsmtpservice.entity.*;

import java.util.List;

public interface AlgorithmService {
    Algorithm createAlgorithm(Algorithm algorithm);
    List<Algorithm> getAllAlgorithms();
    Algorithm getAlgorithmById(Long id);
    Algorithm getAlgorithmByUID(String uid);
    Algorithm updateAlgorithm(Long id, Algorithm algorithm);
    void deleteAlgorithm(Long id); // 返回类型为void
    AlgorithmRepository getAlgorithmByUid(String uid);
}
