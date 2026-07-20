package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.entity.*;
import com.synapxnet.mlopsdppservice.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 知识库服务
 */
@Service
public class KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KBDocumentMapper documentMapper;

    @Autowired
    private KBChunkMapper chunkMapper;

    @Autowired
    private EmbeddingModelMapper embeddingModelMapper;

    // ==================== 知识库管理 ====================

    public List<KnowledgeBase> findAll(String status) {
        return knowledgeBaseMapper.findAll(status, null);
    }

    public KnowledgeBase findById(Long id) {
        return knowledgeBaseMapper.findById(id);
    }

    @Transactional
    public KnowledgeBase create(KnowledgeBase kb) {
        kb.setUid(UUID.randomUUID().toString());
        kb.setStatus("active");
        kb.setDoc_count(0);
        kb.setChunk_count(0);
        kb.setTotal_tokens(0L);
        kb.setTotal_size_bytes(0L);

        // 如果未指定嵌入模型，使用默认模型
        if (kb.getEmbedding_model_id() == null) {
            EmbeddingModel defaultModel = embeddingModelMapper.findDefault();
            if (defaultModel != null) {
                kb.setEmbedding_model_id(defaultModel.getId());
                kb.setEmbedding_provider(defaultModel.getProvider());
                kb.setEmbedding_model(defaultModel.getModel_name());
                kb.setEmbedding_dimension(defaultModel.getDimension());
            }
        }

        // 设置默认值
        if (kb.getVector_db_type() == null) kb.setVector_db_type("milvus");
        if (kb.getChunk_strategy() == null) kb.setChunk_strategy("recursive");
        if (kb.getChunk_size() == null) kb.setChunk_size(500);
        if (kb.getChunk_overlap() == null) kb.setChunk_overlap(50);
        if (kb.getRetrieval_method() == null) kb.setRetrieval_method("hybrid");
        if (kb.getTop_k() == null) kb.setTop_k(5);
        if (kb.getVisibility() == null) kb.setVisibility("private");

        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    @Transactional
    public KnowledgeBase update(KnowledgeBase kb) {
        knowledgeBaseMapper.update(kb);
        return knowledgeBaseMapper.findById(kb.getId());
    }

    @Transactional
    public void delete(Long id) {
        // 删除相关分块
        chunkMapper.deleteByKbId(id);
        // 删除相关文档
        documentMapper.deleteByKbId(id);
        // 删除知识库
        knowledgeBaseMapper.deleteById(id);
    }

    @Transactional
    public void rebuild(Long id) {
        knowledgeBaseMapper.updateStatus(id, "indexing");
        // TODO: 触发异步重建任务
    }

    public void updateStats(Long kbId) {
        int docCount = documentMapper.countByKbId(kbId);
        int chunkCount = chunkMapper.countByKbId(kbId);
        Long totalTokens = chunkMapper.sumTokensByKbId(kbId);
        Long totalSize = documentMapper.sumFileSizeByKbId(kbId);

        knowledgeBaseMapper.updateStats(kbId, docCount, chunkCount,
            totalTokens != null ? totalTokens : 0L,
            totalSize != null ? totalSize : 0L);
    }

    // ==================== 文档管理 ====================

    public List<KBDocument> findDocuments(Long kbId, String status) {
        return documentMapper.findByKbId(kbId, status);
    }

    public KBDocument findDocument(Long docId) {
        return documentMapper.findById(docId);
    }

    @Transactional
    public KBDocument uploadDocument(Long kbId, MultipartFile file, Integer customChunkSize, Integer customChunkOverlap) {
        KBDocument doc = new KBDocument();
        doc.setUid(UUID.randomUUID().toString());
        doc.setKb_id(kbId);
        doc.setName(file.getOriginalFilename());
        doc.setOriginal_name(file.getOriginalFilename());
        doc.setFile_size(file.getSize());
        doc.setMime_type(file.getContentType());
        doc.setStatus("pending");
        doc.setProcess_progress(0);
        doc.setSource_type("upload");
        doc.setWord_count(0);
        doc.setChar_count(0);
        doc.setChunk_count(0);
        doc.setToken_count(0);

        // 提取文件类型
        String filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            doc.setType(filename.substring(filename.lastIndexOf(".") + 1).toLowerCase());
        }

        if (customChunkSize != null) doc.setCustom_chunk_size(customChunkSize);
        if (customChunkOverlap != null) doc.setCustom_chunk_overlap(customChunkOverlap);

        // TODO: 保存文件到存储系统
        // doc.setFile_path(savedPath);

        documentMapper.insert(doc);

        // 触发异步处理任务
        // TODO: processDocumentAsync(doc);

        return doc;
    }

    @Transactional
    public void deleteDocument(Long kbId, Long docId) {
        // 删除分块
        chunkMapper.deleteByDocId(docId);
        // 删除文档
        documentMapper.deleteById(docId);
        // 更新知识库统计
        updateStats(kbId);
    }

    @Transactional
    public void reindexDocument(Long kbId, Long docId) {
        // 删除现有分块
        chunkMapper.deleteByDocId(docId);
        // 更新状态
        documentMapper.updateProcessStatus(docId, "processing", 0);
        // TODO: 触发异步重新索引任务
    }

    // ==================== 分块管理 ====================

    public List<KBChunk> findChunks(Long kbId, Long docId) {
        return chunkMapper.findByDocId(docId);
    }

    // ==================== 嵌入模型管理 ====================

    public List<EmbeddingModel> findEmbeddingModels() {
        return embeddingModelMapper.findAllActive();
    }

    public EmbeddingModel findEmbeddingModel(Long id) {
        return embeddingModelMapper.findById(id);
    }

    @Transactional
    public EmbeddingModel createEmbeddingModel(EmbeddingModel model) {
        model.setStatus("active");
        if (model.getIs_default() != null && model.getIs_default()) {
            embeddingModelMapper.clearDefault();
        }
        embeddingModelMapper.insert(model);
        return model;
    }

    @Transactional
    public EmbeddingModel updateEmbeddingModel(EmbeddingModel model) {
        if (model.getIs_default() != null && model.getIs_default()) {
            embeddingModelMapper.clearDefault();
        }
        embeddingModelMapper.update(model);
        return embeddingModelMapper.findById(model.getId());
    }

    @Transactional
    public void deleteEmbeddingModel(Long id) {
        embeddingModelMapper.deleteById(id);
    }
}
