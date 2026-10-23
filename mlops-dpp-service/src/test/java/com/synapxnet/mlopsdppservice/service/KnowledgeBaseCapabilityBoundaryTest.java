/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 知识库能力边界回归。 Knowledge-base capability boundary regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.controller.KnowledgeBaseController;
import com.synapxnet.mlopsdppservice.entity.KBDocument;
import com.synapxnet.mlopsdppservice.entity.KnowledgeBase;
import com.synapxnet.mlopsdppservice.mapper.KBChunkMapper;
import com.synapxnet.mlopsdppservice.mapper.KBDocumentMapper;
import com.synapxnet.mlopsdppservice.mapper.KnowledgeBaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeBaseCapabilityBoundaryTest {
    private KnowledgeBaseService service;
    private KnowledgeBaseMapper knowledgeBases;
    private KBDocumentMapper documents;
    private KBChunkMapper chunks;

    /** 提供真实服务及受控持久层边界。 Provide the real service with controlled persistence boundaries. */
    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseService();
        knowledgeBases = mock(KnowledgeBaseMapper.class);
        documents = mock(KBDocumentMapper.class);
        chunks = mock(KBChunkMapper.class);
        ReflectionTestUtils.setField(service, "knowledgeBaseMapper", knowledgeBases);
        ReflectionTestUtils.setField(service, "documentMapper", documents);
        ReflectionTestUtils.setField(service, "chunkMapper", chunks);
        when(knowledgeBases.findById(1L)).thenReturn(new KnowledgeBase());
        KBDocument doc = new KBDocument();
        doc.setId(2L);
        doc.setKb_id(1L);
        when(documents.findById(2L)).thenReturn(doc);
    }

    /** 未实现处理时不写入永久等待的文档或删掉旧索引。 Do not write permanently pending documents or destroy an existing index. */
    @Test
    void unavailableProcessingLeavesExistingRecordsUntouched() {
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.uploadDocument(
                1L, new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes()), null, null)).getStatusCode().value());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.rebuild(1L)).getStatusCode().value());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.reindexDocument(1L, 2L)).getStatusCode().value());
        verify(knowledgeBases, times(2)).findById(1L);
        verify(documents).findById(2L);
        verifyNoMoreInteractions(knowledgeBases, documents);
        verifyNoInteractions(chunks);
    }

    /** 拒绝跨知识库的文档读取和删除。 Reject document reads and deletion under another knowledge-base ID. */
    @Test
    void rejectsCrossKnowledgeBaseDocumentAccess() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.deleteDocument(9L, 2L)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.findChunks(9L, 2L)).getStatusCode().value());
        verify(documents, times(2)).findById(2L);
        verifyNoMoreInteractions(documents);
        verifyNoInteractions(chunks);
    }

    /** HTTP接口明确返回503，不以空结果冒充已执行检索。 Return HTTP 503 instead of pretending an empty retrieval was executed. */
    @Test
    void unsupportedHttpCapabilitiesReturnRetryableErrors() throws Exception {
        KnowledgeBaseController controller = new KnowledgeBaseController();
        ReflectionTestUtils.setField(controller, "knowledgeBaseService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String endpoint : new String[]{"/knowledge-bases/1/retrieve", "/knowledge-bases/1/retrieve/test", "/embedding-models/1/test"}) {
            mvc.perform(post("/api/dpp" + endpoint).contentType("application/json").content("{\"query\":\"test\"}"))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        }
        mvc.perform(get("/api/dpp/knowledge-bases/1/retrieval-logs"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
        verifyNoInteractions(knowledgeBases, documents, chunks);
    }
}
