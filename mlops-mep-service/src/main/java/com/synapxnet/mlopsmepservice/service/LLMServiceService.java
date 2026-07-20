package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.LLMService;
import com.synapxnet.mlopsmepservice.mapper.LLMServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LLMServiceService {

    private final LLMServiceMapper llmServiceMapper;
    private final WebClient.Builder webClientBuilder;

    public List<LLMService> findAll() {
        return llmServiceMapper.findAll();
    }

    public LLMService findById(Long id) {
        return llmServiceMapper.findById(id);
    }

    public LLMService create(LLMService service) {
        service.setUid(UUID.randomUUID().toString());
        service.setStatus("stopped");
        service.setCreatedAt(LocalDateTime.now());
        llmServiceMapper.insert(service);
        return service;
    }

    public LLMService update(LLMService service) {
        service.setUpdatedAt(LocalDateTime.now());
        llmServiceMapper.update(service);
        return llmServiceMapper.findById(service.getId());
    }

    public void delete(Long id) {
        LLMService service = llmServiceMapper.findById(id);
        if (service != null && "running".equals(service.getStatus())) {
            stop(id);
        }
        llmServiceMapper.deleteById(id);
    }

    public void start(Long id) {
        llmServiceMapper.updateStatus(id, "running");
    }

    public void stop(Long id) {
        llmServiceMapper.updateStatus(id, "stopped");
    }

    public Map<String, Object> testConnection(String endpoint, String apiKey, String type) {
        try {
            WebClient client = webClientBuilder.baseUrl(endpoint).build();

            String testUrl;
            switch (type) {
                case "ollama":
                    testUrl = "/api/tags";
                    break;
                case "openai":
                case "deepseek":
                    testUrl = "/models";
                    break;
                default:
                    testUrl = "/health";
            }

            var response = client.get()
                .uri(testUrl)
                .headers(headers -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .retrieve()
                .toBodilessEntity()
                .block();

            return Map.of(
                "success", true,
                "message", "连接成功"
            );
        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage());
            return Map.of(
                "success", false,
                "message", "连接失败: " + e.getMessage()
            );
        }
    }
}
