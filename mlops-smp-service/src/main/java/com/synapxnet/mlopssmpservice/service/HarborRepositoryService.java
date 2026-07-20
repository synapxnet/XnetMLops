package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.HarborRepository;
import com.synapxnet.mlopssmpservice.mapper.HarborRepositoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HarborRepositoryService {

    private final HarborRepositoryMapper repositoryMapper;

    @Autowired
    public HarborRepositoryService(HarborRepositoryMapper repositoryMapper) {
        this.repositoryMapper = repositoryMapper;
    }

    @Transactional
    public HarborRepository createRepository(HarborRepository repository) {
        repository.setUid(UUID.randomUUID().toString());
        repositoryMapper.insert(repository);
        return repository;
    }

    public List<HarborRepository> getAllRepositories() {
        return repositoryMapper.findAll();
    }

    public Optional<HarborRepository> getRepositoryById(Integer id) {
        return repositoryMapper.findById(id);
    }

    public Optional<HarborRepository> getRepositoryByUid(String uid) {
        return repositoryMapper.findByUid(uid);
    }

    @Transactional
    public HarborRepository updateRepository(Integer id, HarborRepository repository) {
        repository.setId(id);
        repositoryMapper.update(repository);
        return repositoryMapper.findById(id)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
    }

    @Transactional
    public void deleteRepository(Integer id) {
        repositoryMapper.deleteById(id);
    }
}
