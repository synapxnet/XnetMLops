package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.DockerFile;
import com.synapxnet.mlopssmpservice.mapper.DockerFileMapper;
import com.synapxnet.mlopssmpservice.mapper.HarborRepositoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DockerFileService {

    private final DockerFileMapper dockerFileMapper;
    private final HarborRepositoryMapper harborRepositoryMapper;

    @Autowired
    public DockerFileService(DockerFileMapper dockerFileMapper,
                             HarborRepositoryMapper harborRepositoryMapper) {
        this.dockerFileMapper = dockerFileMapper;
        this.harborRepositoryMapper = harborRepositoryMapper;
    }

    @Transactional
    public DockerFile createDockerFile(DockerFile dockerFile) {
        // 验证关联的Harbor仓库存在
        harborRepositoryMapper.findByUid(dockerFile.getHarbor_uid())
                .orElseThrow(() -> new RuntimeException("Harbor repository not found"));

        dockerFile.setUid(UUID.randomUUID().toString());
        dockerFile.setPush_status(DockerFile.PushStatus.PENDING);
        dockerFileMapper.insert(dockerFile);
        return dockerFile;
    }

    public List<DockerFile> getAllDockerFiles() {
        return dockerFileMapper.findAll();
    }

    public Optional<DockerFile> getDockerFileById(Integer id) {
        return dockerFileMapper.findById(id);
    }

    public Optional<DockerFile> getDockerFileByUID(String uid) {
        return dockerFileMapper.findByUID(uid);
    }

    public List<DockerFile> getDockerFilesByHarborUid(String harborUid) {
        return dockerFileMapper.findByHarborUid(harborUid);
    }

    @Transactional
    public DockerFile updateDockerFile(Integer id, DockerFile dockerFile) {
        dockerFile.setId(id);
        dockerFileMapper.update(dockerFile);
        return dockerFileMapper.findById(id)
                .orElseThrow(() -> new RuntimeException("Docker file not found"));
    }

    @Transactional
    public void updatePushStatus(String uid, DockerFile.PushStatus status, String pushHistory) {
        dockerFileMapper.updatePushStatus(uid, status, pushHistory);
    }

    @Transactional
    public void deleteDockerFile(Integer id) {
        dockerFileMapper.deleteById(id);
    }
}