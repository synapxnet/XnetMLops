package com.synapxnet.mlopsmepservice.controller;

import com.synapxnet.mlopsmepservice.service.MTPModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mep/mtp-models")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MTPModelController {

    private final MTPModelService mtpModelService;

    /**
     * 按当前租户读取 MTP 训练任务产生的模型输出，防止跨租户读取或落入 default 租户。
     *
     * @param tenantUid 当前登录上下文中的租户唯一标识
     * @return 当前租户可部署的模型输出列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMTPOutputModels(
            @RequestHeader("X-Tenant-Uid") String tenantUid) {
        List<Map<String, Object>> models = mtpModelService.getOutputModels(tenantUid);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", models
        ));
    }
}
