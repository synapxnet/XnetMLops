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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMTPOutputModels() {
        List<Map<String, Object>> models = mtpModelService.getOutputModels();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", models
        ));
    }
}
