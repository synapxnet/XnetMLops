package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.entity.*;
import com.synapxnet.mlopsdppservice.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

    /** 未接入真实索引执行器前不改变索引状态。 Preserve index state until a real indexing executor is available. */
    public void rebuild(Long id) {
        requireKnowledgeBase(id);
        throw processingUnavailable();
    }

    /** 显式拒绝尚未接入的处理能力。 Reject processing capabilities that have not been connected. */
    public ResponseStatusException processingUnavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "知识库文档处理与向量检索服务尚未接入，请先配置处理服务；现有知识库和文档记录仍可查看。");
    }

    /** 校验知识库存在，避免错误写入不存在的目标。 Validate the knowledge-base target before any mutation. */
    private void requireKnowledgeBase(Long id) {
        if (knowledgeBaseMapper.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
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

    /** 未持久化文件和运行处理器时禁止伪造上传成功。 Never acknowledge an upload without durable storage and an actual processor. */
    public KBDocument uploadDocument(Long kbId, MultipartFile file, Integer customChunkSize, Integer customChunkOverlap) {
        requireKnowledgeBase(kbId);
        throw processingUnavailable();
    }

    /** 按知识库限定文档，防止跨知识库读取或删除。 Scope document access to its owning knowledge base. */
    public KBDocument findDocument(Long kbId, Long docId) {
        KBDocument doc = documentMapper.findById(docId);
        if (doc == null || !java.util.Objects.equals(kbId, doc.getKb_id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在或不属于当前知识库");
        }
        return doc;
    }

    @Transactional
    public void deleteDocument(Long kbId, Long docId) {
        findDocument(kbId, docId);
        // 删除分块
        chunkMapper.deleteByDocId(docId);
        // 删除文档
        documentMapper.deleteById(docId);
        // 更新知识库统计
        updateStats(kbId);
    }

    /** 重建不可用时保留已有分块及处理状态。 Preserve existing chunks and status when reindexing is unavailable. */
    public void reindexDocument(Long kbId, Long docId) {
        findDocument(kbId, docId);
        throw processingUnavailable();
    }

    // ==================== 分块管理 ====================

    public List<KBChunk> findChunks(Long kbId, Long docId) {
        findDocument(kbId, docId);
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
