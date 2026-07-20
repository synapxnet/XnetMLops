package com.synapxnet.mlopsxaaservice.controller;

import com.synapxnet.mlopsxaaservice.entity.Assistant;
import com.synapxnet.mlopsxaaservice.entity.AssistantConversation;
import com.synapxnet.mlopsxaaservice.entity.AssistantMessage;
import com.synapxnet.mlopsxaaservice.service.AssistantService;
import com.synapxnet.mlopsxaaservice.service.ExternalServiceClient;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 智能助手控制器
 * 提供智能助手的CRUD操作和会话管理
 */
@RestController
@RequestMapping("api/xaa/assistants")
@CrossOrigin(origins = "*")
public class AssistantController {

    private static final Logger log = LoggerFactory.getLogger(AssistantController.class);

    private final AssistantService assistantService;
    private final ExternalServiceClient externalServiceClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${xaa.services.mep-url:http://127.0.0.1:8184}")
    private String mepServiceUrl;

    public AssistantController(AssistantService assistantService,
                               ExternalServiceClient externalServiceClient) {
        this.assistantService = assistantService;
        this.externalServiceClient = externalServiceClient;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 助手管理 ====================

    /**
     * 获取所有助手
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAssistants() {
        List<Assistant> assistants = assistantService.findAll();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", assistants
        ));
    }

    /**
     * 获取所有激活的助手
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveAssistants() {
        List<Assistant> assistants = assistantService.findActive();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", assistants
        ));
    }

    /**
     * 获取默认助手
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefaultAssistant() {
        Assistant assistant = assistantService.findDefault();
        if (assistant == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "未设置默认助手"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", assistant
        ));
    }

    /**
     * 获取助手详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAssistantById(@PathVariable Long id) {
        Assistant assistant = assistantService.findById(id);
        if (assistant == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "助手不存在"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", assistant
        ));
    }

    /**
     * 获取助手配置（用于前端浮窗）
     */
    @GetMapping("/{id}/config")
    public ResponseEntity<Map<String, Object>> getAssistantConfig(@PathVariable Long id) {
        Map<String, Object> config = assistantService.getAssistantConfig(id);
        if (config == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "助手不存在"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", config
        ));
    }

    /**
     * 创建助手
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAssistant(@RequestBody Assistant assistant) {
        Assistant created = assistantService.create(assistant);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "创建成功",
            "data", created
        ));
    }

    /**
     * 更新助手
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAssistant(
            @PathVariable Long id,
            @RequestBody Assistant assistant) {
        assistant.setId(id);
        Assistant updated = assistantService.update(assistant);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "更新成功",
            "data", updated
        ));
    }

    /**
     * 删除助手
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAssistant(@PathVariable Long id) {
        assistantService.delete(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "删除成功"
        ));
    }

    /**
     * 启用/禁用助手
     */
    @PostMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        assistantService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "状态更新成功"
        ));
    }

    /**
     * 设为默认助手
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<Map<String, Object>> setDefault(@PathVariable Long id) {
        assistantService.setDefault(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "设置成功"
        ));
    }

    // ==================== 会话管理 ====================

    /**
     * 获取助手的会话列表
     */
    @GetMapping("/{id}/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(@PathVariable Long id) {
        List<AssistantConversation> conversations = assistantService.getConversations(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", conversations
        ));
    }

    /**
     * 创建新会话
     */
    @PostMapping("/{id}/conversations")
    public ResponseEntity<Map<String, Object>> createConversation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String userName = body.get("userName");
        AssistantConversation conversation = assistantService.createConversation(id, userId, userName);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "创建成功",
            "data", conversation
        ));
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/conversations/{convId}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable Long convId) {
        AssistantConversation conversation = assistantService.getConversation(convId);
        if (conversation == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "会话不存在"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", conversation
        ));
    }

    /**
     * 归档会话
     */
    @PostMapping("/conversations/{convId}/archive")
    public ResponseEntity<Map<String, Object>> archiveConversation(@PathVariable Long convId) {
        assistantService.archiveConversation(convId);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "归档成功"
        ));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{convId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable Long convId) {
        assistantService.deleteConversation(convId);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "删除成功"
        ));
    }

    // ==================== 消息管理 ====================

    /**
     * 获取会话消息
     */
    @GetMapping("/conversations/{convId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable Long convId,
            @RequestParam(defaultValue = "50") int limit) {
        List<AssistantMessage> messages;
        if (limit > 0) {
            messages = assistantService.getRecentMessages(convId, limit);
        } else {
            messages = assistantService.getMessages(convId);
        }
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", messages
        ));
    }

    /**
     * 添加消息
     */
    @PostMapping("/conversations/{convId}/messages")
    public ResponseEntity<Map<String, Object>> addMessage(
            @PathVariable Long convId,
            @RequestBody Map<String, Object> body) {
        String role = (String) body.get("role");
        String content = (String) body.get("content");
        Integer tokenCount = body.get("tokenCount") != null ? ((Number) body.get("tokenCount")).intValue() : null;
        String modelUsed = (String) body.get("modelUsed");
        String ragSources = (String) body.get("ragSources");
        String toolCalls = (String) body.get("toolCalls");

        AssistantMessage message = assistantService.addMessage(
            convId, role, content, tokenCount, modelUsed, ragSources, toolCalls
        );
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", message
        ));
    }

    /**
     * 获取用户的所有会话
     */
    @GetMapping("/user/{userId}/conversations")
    public ResponseEntity<Map<String, Object>> getUserConversations(@PathVariable String userId) {
        List<AssistantConversation> conversations = assistantService.getUserConversations(userId);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", conversations
        ));
    }

    // ==================== Chat Completions 代理 ====================

    /**
     * 代理转发 OpenClaw Gateway 的 chat/completions 请求（SSE 流式）
     * 解决前端直接调用外部 Gateway 时的 CORS 问题
     * 使用 HttpServletResponse 直接写原始字节流，避免 Spring SSE 二次包装
     */
    @PostMapping("/{id}/chat/completions")
    public void chatCompletions(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletResponse servletResponse) throws IOException {

        servletResponse.setContentType("text/event-stream");
        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.setHeader("Cache-Control", "no-cache");
        servletResponse.setHeader("Connection", "keep-alive");

        OutputStream out = servletResponse.getOutputStream();

        Assistant assistant = assistantService.findById(id);
        if (assistant == null) {
            out.write("data: {\"error\":\"助手不存在\"}\n\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }
        if (assistant.getGatewayUrl() == null || assistant.getGatewayUrl().isBlank()) {
            out.write("data: {\"error\":\"未配置 Gateway URL\"}\n\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }

        String gatewayUrl = assistant.getGatewayUrl().replaceAll("/$", "");
        String resolvedToken = resolveGatewayToken(assistant);

        try {
            // 用 java.net.http.HttpClient 直接获取原始 InputStream，避免 WebClient SSE codec 解析
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(gatewayUrl + "/v1/chat/completions"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

            if (resolvedToken != null && !resolvedToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + resolvedToken);
                requestBuilder.header("X-OpenClaw-Token", resolvedToken);
            }

            log.info("[ChatProxy] 发送请求到 Gateway: {} token={}", gatewayUrl + "/v1/chat/completions",
                    resolvedToken != null ? resolvedToken.substring(0, Math.min(8, resolvedToken.length())) + "..." : "null");

            HttpResponse<InputStream> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());

            log.info("[ChatProxy] Gateway 响应状态: {} headers: {}", response.statusCode(), response.headers().map());

            if (response.statusCode() >= 400) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("[ChatProxy] Gateway 错误: {} body: {}", response.statusCode(), errBody);
                String msg = "data: {\"error\":\"Gateway " + response.statusCode()
                        + ": " + errBody.replace("\"", "\\\"") + "\"}\n\n";
                out.write(msg.getBytes(StandardCharsets.UTF_8));
                out.flush();
                return;
            }

            // 逐块读取原始 SSE 流并透传
            int totalBytes = 0;
            try (InputStream inputStream = response.body()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    log.debug("[ChatProxy] 收到 chunk ({}bytes): {}", bytesRead,
                            chunk.length() > 200 ? chunk.substring(0, 200) + "..." : chunk);
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
            }
            log.info("[ChatProxy] 流式传输完成, 总字节数: {}", totalBytes);
        } catch (Exception e) {
            log.error("[ChatProxy] 代理请求异常: ", e);
            String errorMsg = "data: {\"error\":\"" +
                    (e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "未知错误") + "\"}\n\n";
            out.write(errorMsg.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    /**
     * 解析 Gateway 认证令牌
     * 优先用助手自身配置的 gatewayToken，否则通过 MEP 查询 OpenClaw 实例获取
     * （OpenClaw buildConfigMap 在 token 为空时使用实例 UID 作为 fallback）
     */
    @SuppressWarnings("unchecked")
    private String resolveGatewayToken(Assistant assistant) {
        String token = assistant.getGatewayToken();
        if (token != null && !token.isBlank()) {
            return token;
        }
        if (assistant.getOpenclawInstanceId() == null) {
            return null;
        }
        try {
            Map<String, Object> resp = externalServiceClient.executeHttpRequest(
                    mepServiceUrl + "/mep/openclaw/instances/" + assistant.getOpenclawInstanceId(),
                    "GET", null);
            if (resp != null && resp.get("data") instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                String instanceToken = (String) data.get("gatewayToken");
                if (instanceToken != null && !instanceToken.isBlank()) {
                    return instanceToken;
                }
                return (String) data.get("uid");
            }
        } catch (Exception e) {
            // MEP 服务不可用时忽略
        }
        return null;
    }
}
