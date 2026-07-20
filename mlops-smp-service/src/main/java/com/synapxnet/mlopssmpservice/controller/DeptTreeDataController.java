package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.DeptTreeData;
import com.synapxnet.mlopssmpservice.service.DeptTreeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/smp")
public class DeptTreeDataController {
    private final DeptTreeDataService deptTreeDataService;

    @GetMapping("/dept-tree-data")
    public ResponseEntity<Map<String, Object>> getDeptTreeData() {
        List<DeptTreeData> deptTreeData = deptTreeDataService.getDeptTreeData();

        // 返回统一结构
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", deptTreeData,
                "error", "null"
        ));
    }
}
