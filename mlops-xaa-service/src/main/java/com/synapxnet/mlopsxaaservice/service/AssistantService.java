package com.synapxnet.mlopsxaaservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsxaaservice.entity.Assistant;
import com.synapxnet.mlopsxaaservice.entity.AssistantConversation;
import com.synapxnet.mlopsxaaservice.entity.AssistantMessage;
import com.synapxnet.mlopsxaaservice.mapper.AssistantMapper;
import com.synapxnet.mlopsxaaservice.mapper.AssistantConversationMapper;
import com.synapxnet.mlopsxaaservice.mapper.AssistantMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final AssistantMapper assistantMapper;
    private final AssistantConversationMapper conversationMapper;
    private final AssistantMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    // ==================== 助手管理 ====================

    public List<Assistant> findAll() {
        return assistantMapper.findAll();
    }

    public List<Assistant> findActive() {
        return assistantMapper.findByStatus("active");
    }

    public Assistant findById(Long id) {
        return assistantMapper.findById(id);
    }

    public Assistant findByUid(String uid) {
        return assistantMapper.findByUid(uid);
    }

    public Assistant findDefault() {
        return assistantMapper.findDefault();
    }

    public Assistant create(Assistant assistant) {
        assistant.setUid("ASST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        assistant.setStatus("active");
        assistant.setTemperature(assistant.getTemperature() != null ? assistant.getTemperature() : new BigDecimal("0.70"));
        assistant.setMaxTokens(assistant.getMaxTokens() != null ? assistant.getMaxTokens() : 4096);
        assistant.setRagEnabled(assistant.getRagEnabled() != null ? assistant.getRagEnabled() : false);
        assistant.setRagTopK(assistant.getRagTopK() != null ? assistant.getRagTopK() : 5);
        assistant.setToolsEnabled(assistant.getToolsEnabled() != null ? assistant.getToolsEnabled() : true);
        assistant.setIsDefault(assistant.getIsDefault() != null ? assistant.getIsDefault() : false);
        assistant.setPlaceholder(assistant.getPlaceholder() != null ? assistant.getPlaceholder() : "有什么可以帮助您的?");
        assistant.setTotalConversations(0L);
        assistant.setTotalMessages(0L);
        assistant.setCreatedAt(LocalDateTime.now());

        // 设置默认UI配置
        if (assistant.getUiConfig() == null) {
            try {
                Map<String, Object> defaultUiConfig = Map.of(
                    "position", Map.of("right", 20, "bottom", 20),
                    "size", Map.of("width", 380, "height", 500),
                    "theme", "light",
                    "primaryColor", "#1890ff",
                    "borderRadius", 12,
                    "showAvatar", true
                );
                assistant.setUiConfig(objectMapper.writeValueAsString(defaultUiConfig));
            } catch (Exception e) {
                log.error("设置默认UI配置失败", e);
            }
        }

        assistantMapper.insert(assistant);

        // 如果设为默认，清除其他默认
        if (Boolean.TRUE.equals(assistant.getIsDefault())) {
            assistantMapper.clearDefault();
            assistantMapper.setDefault(assistant.getId());
        }

        return assistant;
    }

    public Assistant update(Assistant assistant) {
        assistant.setUpdatedAt(LocalDateTime.now());
        assistantMapper.update(assistant);

        // 处理默认设置
        if (Boolean.TRUE.equals(assistant.getIsDefault())) {
            assistantMapper.clearDefault();
            assistantMapper.setDefault(assistant.getId());
        }

        return assistantMapper.findById(assistant.getId());
    }

    @Transactional
    public void delete(Long id) {
        // 删除所有相关会话和消息
        List<AssistantConversation> conversations = conversationMapper.findByAssistantId(id);
        for (AssistantConversation conv : conversations) {
            messageMapper.deleteByConversationId(conv.getId());
            conversationMapper.deleteById(conv.getId());
        }
        assistantMapper.deleteById(id);
    }

    public void updateStatus(Long id, String status) {
        assistantMapper.updateStatus(id, status);
    }

    public void setDefault(Long id) {
        assistantMapper.clearDefault();
        assistantMapper.setDefault(id);
    }

    // ==================== 会话管理 ====================

    public List<AssistantConversation> getConversations(Long assistantId) {
        return conversationMapper.findByAssistantId(assistantId);
    }

    public List<AssistantConversation> getUserConversations(String userId) {
        return conversationMapper.findByUserId(userId);
    }

    public AssistantConversation getConversation(Long id) {
        return conversationMapper.findById(id);
    }

    public AssistantConversation createConversation(Long assistantId, String userId, String userName) {
        AssistantConversation conversation = new AssistantConversation();
        conversation.setUid("CONV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        conversation.setAssistantId(assistantId);
        conversation.setUserId(userId);
        conversation.setUserName(userName);
        conversation.setTitle("新对话");
        conversation.setStatus("active");
        conversation.setMessageCount(0);
        conversation.setTokenCount(0L);
        conversation.setCreatedAt(LocalDateTime.now());

        conversationMapper.insert(conversation);
        assistantMapper.incrementConversations(assistantId);

        return conversation;
    }

    public void updateConversationTitle(Long id, String title) {
        AssistantConversation conversation = conversationMapper.findById(id);
        if (conversation != null) {
            conversation.setTitle(title);
            conversationMapper.update(conversation);
        }
    }

    public void archiveConversation(Long id) {
        conversationMapper.updateStatus(id, "archived");
    }

    public void deleteConversation(Long id) {
        messageMapper.deleteByConversationId(id);
        conversationMapper.deleteById(id);
    }

    // ==================== 消息管理 ====================

    public List<AssistantMessage> getMessages(Long conversationId) {
        return messageMapper.findByConversationId(conversationId);
    }

    public List<AssistantMessage> getRecentMessages(Long conversationId, int limit) {
        List<AssistantMessage> messages = messageMapper.findRecentByConversationId(conversationId, limit);
        // 反转顺序，使其按时间正序排列
        Collections.reverse(messages);
        return messages;
    }

    public AssistantMessage addMessage(Long conversationId, String role, String content,
                                        Integer tokenCount, String modelUsed,
                                        String ragSources, String toolCalls) {
        AssistantMessage message = new AssistantMessage();
        message.setUid("MSG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setTokenCount(tokenCount != null ? tokenCount : 0);
        message.setModelUsed(modelUsed);
        message.setRagSources(ragSources);
        message.setToolCalls(toolCalls);
        message.setCreatedAt(LocalDateTime.now());

        messageMapper.insert(message);

        // 更新会话统计
        conversationMapper.incrementMessage(conversationId, message.getTokenCount());

        // 更新助手统计
        AssistantConversation conversation = conversationMapper.findById(conversationId);
        if (conversation != null) {
            assistantMapper.incrementMessages(conversation.getAssistantId());
        }

        // 如果是用户消息且是第一条或第二条，自动更新会话标题
        if ("user".equals(role)) {
            int msgCount = messageMapper.countByConversationId(conversationId);
            if (msgCount <= 2) {
                String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
                updateConversationTitle(conversationId, title);
            }
        }

        return message;
    }

    // ==================== 助手配置解析 ====================

    public Map<String, Object> getAssistantConfig(Long id) {
        Assistant assistant = assistantMapper.findById(id);
        if (assistant == null) {
            return null;
        }

        Map<String, Object> config = new HashMap<>();
        config.put("id", assistant.getId());
        config.put("uid", assistant.getUid());
        config.put("name", assistant.getName());
        config.put("description", assistant.getDescription());
        config.put("avatar", assistant.getAvatar());
        config.put("gatewayUrl", assistant.getGatewayUrl());
        config.put("defaultModel", assistant.getDefaultModel());
        config.put("systemPrompt", assistant.getSystemPrompt());
        config.put("temperature", assistant.getTemperature());
        config.put("maxTokens", assistant.getMaxTokens());
        config.put("ragEnabled", assistant.getRagEnabled());
        config.put("toolsEnabled", assistant.getToolsEnabled());
        config.put("welcomeMessage", assistant.getWelcomeMessage());
        config.put("placeholder", assistant.getPlaceholder());

        // 解析UI配置
        try {
            if (assistant.getUiConfig() != null) {
                config.put("uiConfig", objectMapper.readValue(assistant.getUiConfig(), Map.class));
            }
        } catch (Exception e) {
            log.error("解析UI配置失败", e);
        }

        // 解析技能ID列表
        try {
            if (assistant.getSkillIds() != null) {
                config.put("skillIds", objectMapper.readValue(assistant.getSkillIds(), List.class));
            }
        } catch (Exception e) {
            log.error("解析技能ID列表失败", e);
        }

        // 解析知识库ID列表
        try {
            if (assistant.getKnowledgeBaseIds() != null) {
                config.put("knowledgeBaseIds", objectMapper.readValue(assistant.getKnowledgeBaseIds(), List.class));
            }
        } catch (Exception e) {
            log.error("解析知识库ID列表失败", e);
        }

        return config;
    }
}
