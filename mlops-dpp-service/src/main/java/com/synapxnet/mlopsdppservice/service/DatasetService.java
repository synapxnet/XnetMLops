package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.entity.Dataset;
import java.util.*;

public interface DatasetService {
    Dataset createDataset(Dataset dataset);
    List<Dataset> getAllDatasets();
    Optional<Dataset> findDatasetById(Long id);
    Dataset getDatasetById(Long id);
    boolean deleteDataset(Long id);
    Dataset updateDataset(Long id, Dataset dataset);
    boolean existsByDatasetFile(String dataset_File); // 新增方法
}