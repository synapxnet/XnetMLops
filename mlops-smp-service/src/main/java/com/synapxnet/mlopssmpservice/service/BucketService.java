package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.Utils.HadoopUtil;
import com.synapxnet.mlopssmpservice.entity.Bucket;
import com.synapxnet.mlopssmpservice.mapper.BucketMapper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BucketService {
    private final BucketMapper bucketMapper;
    private final HadoopUtil hadoopUtil;

    @Value("${hdfs.base.path:/buckets}")
    private String hdfsBasePath;

    @Autowired
    public BucketService(BucketMapper bucketMapper, HadoopUtil hadoopUtil) {
        this.bucketMapper = bucketMapper;
        this.hadoopUtil = hadoopUtil;
    }

    // 创建HDFS目录
    private void createHdfsDirectory(String identifier) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            Path path = new Path(hdfsBasePath + "/" + identifier);
            if (!fs.mkdirs(path)) {
                throw new RuntimeException("Failed to create HDFS directory");
            }
        }
    }

    // 递归删除HDFS目录
    private void deleteHdfsDirectory(String identifier) throws Exception {
        try (FileSystem fs = hadoopUtil.getFileSystem()) {
            Path path = new Path(hdfsBasePath + "/" + identifier);
            if (fs.exists(path)) {
                if (!fs.delete(path, true)) {
                    throw new RuntimeException("Failed to delete HDFS directory");
                }
            } else {
                throw new FileNotFoundException("HDFS directory not found");
            }
        }
    }

    @Transactional
    public Bucket saveBucket(Bucket bucket) {
        boolean isNew = bucket.getId() == null;

        // 设置公共字段
        Date now = new Date();
        if (isNew) {
            bucket.setUid(UUID.randomUUID().toString());
            bucket.setCreated_at(now);
            bucket.setStatus("active");
        }
        bucket.setUpdated_at(now);

        // 设置默认值
        if (bucket.getCurrent_size() == null) bucket.setCurrent_size(0.0);
        if (bucket.getMax_size() == null) bucket.setMax_size(1024.0); // 默认1GB

        if (isNew) {
            // 新增存储桶
            bucketMapper.insertBucket(bucket);
            try {
                createHdfsDirectory(bucket.getIdentifier());
            } catch (Exception e) {
                // HDFS创建失败时回滚数据库操作
                bucketMapper.deleteBucketById(bucket.getId());
                throw new RuntimeException("HDFS directory creation failed: " + e.getMessage());
            }
            return bucketMapper.selectBucketById(bucket.getId());
        } else {
            // 更新存储桶
            bucketMapper.updateBucket(bucket);
            return bucketMapper.selectBucketById(bucket.getId());
        }
    }

    @Transactional
    public boolean deleteBucket(Long id) {
        Bucket bucket = bucketMapper.selectBucketById(id);
        if (bucket == null) {
            return false;
        }

        try {
            deleteHdfsDirectory(bucket.getIdentifier());
        } catch (FileNotFoundException e) {
            // 忽略目录不存在的异常
        } catch (Exception e) {
            throw new RuntimeException("HDFS deletion failed: " + e.getMessage());
        }

        int affectedRows = bucketMapper.deleteBucketById(id);
        return affectedRows > 0;
    }

    public List<Bucket> getAllBuckets() {
        return bucketMapper.selectBucketsWithOrgInfo();
    }

    public Optional<Bucket> findBucketById(Long id) {
        return Optional.ofNullable(bucketMapper.findById(id).orElse(null));
    }

    public Bucket getBucketById(Long id) {
        Bucket bucket = bucketMapper.selectBucketById(id);
        if (bucket == null) {
            throw new RuntimeException("Bucket not found with id: " + id);
        }
        return bucket;
    }

    public Bucket updateBucket(Long id, Bucket bucketDetails) {
        Bucket existingBucket = getBucketById(id);

        // 更新可修改字段
        existingBucket.setName(bucketDetails.getName());
        existingBucket.setType(bucketDetails.getType());
        existingBucket.setTenant_uid(bucketDetails.getTenant_uid());
        existingBucket.setDept_uid(bucketDetails.getDept_uid());
        existingBucket.setTeam_uid(bucketDetails.getTeam_uid());
        existingBucket.setCurrent_size(bucketDetails.getCurrent_size());
        existingBucket.setMax_size(bucketDetails.getMax_size());
        existingBucket.setStatus(bucketDetails.getStatus());
        existingBucket.setAccess_key(bucketDetails.getAccess_key());
        existingBucket.setAuthorized_tenants(bucketDetails.getAuthorized_tenants());
        existingBucket.setUpdated_at(new Date());

        bucketMapper.updateBucket(existingBucket);
        return bucketMapper.selectBucketById(id);
    }
}