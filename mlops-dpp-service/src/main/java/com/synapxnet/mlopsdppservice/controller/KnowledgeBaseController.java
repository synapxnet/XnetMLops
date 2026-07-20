package com.synapxnet.mlopsdppservice.controller;

import com.synapxnet.mlopsdppservice.entity.*;
import com.synapxnet.mlopsdppservice.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/dpp")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    // 统一响应格式工具类
    private static ResponseEntity<Map<String, Object>> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    private static ResponseEntity<Map<String, Object>> success() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", null);
        return ResponseEntity.ok(result);
    }

    private static ResponseEntity<Map<String, Object>> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        return ResponseEntity.ok(result);
    }

    // ==================== 知识库管理 ====================

    @GetMapping("/knowledge-bases")
    public ResponseEntity<Map<String, Object>> getKnowledgeBases(
            @RequestParam(value = "status", required = false) String status) {
        return success(knowledgeBaseService.findAll(status));
    }

    @GetMapping("/knowledge-bases/{id}")
    public ResponseEntity<Map<String, Object>> getKnowledgeBase(@PathVariable("id") Long id) {
        KnowledgeBase kb = knowledgeBaseService.findById(id);
        if (kb == null) {
            return error(404, "知识库不存在");
        }
        return success(kb);
    }

    @PostMapping("/knowledge-bases")
    public ResponseEntity<Map<String, Object>> createKnowledgeBase(@RequestBody KnowledgeBase kb) {
        return success(knowledgeBaseService.create(kb));
    }

    @PutMapping("/knowledge-bases/{id}")
    public ResponseEntity<Map<String, Object>> updateKnowledgeBase(
            @PathVariable("id") Long id, @RequestBody KnowledgeBase kb) {
        kb.setId(id);
        return success(knowledgeBaseService.update(kb));
    }

    @DeleteMapping("/knowledge-bases/{id}")
    public ResponseEntity<Map<String, Object>> deleteKnowledgeBase(@PathVariable("id") Long id) {
        knowledgeBaseService.delete(id);
        return success();
    }

    @PostMapping("/knowledge-bases/{id}/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildKnowledgeBase(@PathVariable("id") Long id) {
        knowledgeBaseService.rebuild(id);
        return success();
    }

    // ==================== 文档管理 ====================

    @GetMapping("/knowledge-bases/{kbId}/documents")
    public ResponseEntity<Map<String, Object>> getDocuments(
            @PathVariable("kbId") Long kbId,
            @RequestParam(value = "status", required = false) String status) {
        return success(knowledgeBaseService.findDocuments(kbId, status));
    }

    @GetMapping("/knowledge-bases/{kbId}/documents/{docId}")
    public ResponseEntity<Map<String, Object>> getDocument(
            @PathVariable("kbId") Long kbId, @PathVariable("docId") Long docId) {
        KBDocument doc = knowledgeBaseService.findDocument(docId);
        if (doc == null) {
            return error(404, "文档不存在");
        }
        return success(doc);
    }

    @PostMapping("/knowledge-bases/{kbId}/documents")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable("kbId") Long kbId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "customChunkSize", required = false) Integer customChunkSize,
            @RequestParam(value = "customChunkOverlap", required = false) Integer customChunkOverlap) {
        return success(knowledgeBaseService.uploadDocument(
                kbId, file, customChunkSize, customChunkOverlap));
    }

    @DeleteMapping("/knowledge-bases/{kbId}/documents/{docId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable("kbId") Long kbId, @PathVariable("docId") Long docId) {
        knowledgeBaseService.deleteDocument(kbId, docId);
        return success();
    }

    @PostMapping("/knowledge-bases/{kbId}/documents/{docId}/reindex")
    public ResponseEntity<Map<String, Object>> reindexDocument(
            @PathVariable("kbId") Long kbId, @PathVariable("docId") Long docId) {
        knowledgeBaseService.reindexDocument(kbId, docId);
        return success();
    }

    // ==================== 分块查看 ====================

    @GetMapping("/knowledge-bases/{kbId}/documents/{docId}/chunks")
    public ResponseEntity<Map<String, Object>> getChunks(
            @PathVariable("kbId") Long kbId, @PathVariable("docId") Long docId) {
        return success(knowledgeBaseService.findChunks(kbId, docId));
    }

    // ==================== 检索 ====================

    @PostMapping("/knowledge-bases/{kbId}/retrieve")
    public ResponseEntity<Map<String, Object>> retrieve(
            @PathVariable("kbId") Long kbId,
            @RequestBody Map<String, Object> request) {
        // TODO: 实现实际的向量检索逻辑
        Map<String, Object> response = new HashMap<>();
        response.put("results", List.of());
        response.put("totalLatencyMs", 0);
        response.put("embeddingLatencyMs", 0);
        response.put("retrievalLatencyMs", 0);
        return success(response);
    }

    @PostMapping("/knowledge-bases/{kbId}/retrieve/test")
    public ResponseEntity<Map<String, Object>> retrieveTest(
            @PathVariable("kbId") Long kbId,
            @RequestBody Map<String, Object> request) {
        // TODO: 实现测试检索逻辑
        Map<String, Object> response = new HashMap<>();
        response.put("results", List.of());
        response.put("totalLatencyMs", 0);
        response.put("embeddingLatencyMs", 0);
        response.put("retrievalLatencyMs", 0);
        return success(response);
    }

    @GetMapping("/knowledge-bases/{kbId}/retrieval-logs")
    public ResponseEntity<Map<String, Object>> getRetrievalLogs(
            @PathVariable("kbId") Long kbId,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit) {
        // TODO: 实现检索日志查询
        return success(List.of());
    }

    // ==================== 嵌入模型管理 ====================

    @GetMapping("/embedding-models")
    public ResponseEntity<Map<String, Object>> getEmbeddingModels() {
        return success(knowledgeBaseService.findEmbeddingModels());
    }

    @GetMapping("/embedding-models/{id}")
    public ResponseEntity<Map<String, Object>> getEmbeddingModel(@PathVariable("id") Long id) {
        EmbeddingModel model = knowledgeBaseService.findEmbeddingModel(id);
        if (model == null) {
            return error(404, "嵌入模型不存在");
        }
        return success(model);
    }

    @PostMapping("/embedding-models")
    public ResponseEntity<Map<String, Object>> createEmbeddingModel(@RequestBody EmbeddingModel model) {
        return success(knowledgeBaseService.createEmbeddingModel(model));
    }

    @PutMapping("/embedding-models/{id}")
    public ResponseEntity<Map<String, Object>> updateEmbeddingModel(
            @PathVariable("id") Long id, @RequestBody EmbeddingModel model) {
        model.setId(id);
        return success(knowledgeBaseService.updateEmbeddingModel(model));
    }

    @DeleteMapping("/embedding-models/{id}")
    public ResponseEntity<Map<String, Object>> deleteEmbeddingModel(@PathVariable("id") Long id) {
        knowledgeBaseService.deleteEmbeddingModel(id);
        return success();
    }

    @PostMapping("/embedding-models/{id}/test")
    public ResponseEntity<Map<String, Object>> testEmbeddingModel(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {
        // TODO: 实现嵌入模型测试
        Map<String, Object> response = new HashMap<>();
        response.put("embedding", new float[0]);
        response.put("latencyMs", 0);
        return success(response);
    }
}
