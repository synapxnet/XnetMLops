/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 用途：数据集配置标签更新的HTTP契约回归。Purpose: Dataset configuration label update HTTP contract regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-14
 * Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.mapper.DatasetConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DatasetConfigControllerTest {
    private DatasetConfigMapper mapper;
    private MockMvc mvc;

    /** 通过反射注入隔离Mapper，不启动数据库或线上服务。Inject an isolated mapper through reflection without a database or online service. */
    @BeforeEach
    void setUp() {
        mapper = mock(DatasetConfigMapper.class);
        DatasetConfigController controller = new DatasetConfigController();
        ReflectionTestUtils.setField(controller, "datasetConfigMapper", mapper);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** 类型和区域均只更新标签，保留路径中的业务引用键。Both type and zone updates change only the label and retain the path reference key. */
    @ParameterizedTest
    @CsvSource({"datasetTypes,DATASET_TYPE", "datasetZones,DATASET_ZONE"})
    void updatesTrimmedLabelWithoutReplacingReferenceKey(String type, String storedType) throws Exception {
        when(mapper.updateDatasetConfigLabel("fixture label", "fixture-value", storedType)).thenReturn(1);
        mvc.perform(put("/api/smp/dataset-config/{type}/{value}", type, "fixture-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"  fixture label  \",\"value\":\"different-body-value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.label").value("fixture label"))
                .andExpect(jsonPath("$.data.value").value("fixture-value"));
        verify(mapper).countOtherLabels("fixture label", "fixture-value", storedType);
        verify(mapper).updateDatasetConfigLabel("fixture label", "fixture-value", storedType);
    }

    /** 未定义配置类型在访问持久层之前被拒绝。Reject undefined configuration types before persistence access. */
    @Test
    void rejectsInvalidTypeBeforeAccessingMapper() throws Exception {
        mvc.perform(put("/api/smp/dataset-config/unknown/fixture-value")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"fixture label\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(mapper);
    }

    /** 空白和缺失标签不能被保存为空配置名称。Blank and missing labels cannot be saved as empty configuration names. */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"label\":null}", "{\"label\":\"\"}", "{\"label\":\"   \"}"})
    void rejectsEmptyLabelsBeforeAccessingMapper(String payload) throws Exception {
        mvc.perform(put("/api/smp/dataset-config/datasetTypes/fixture-value")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(mapper);
    }

    /** 重复标签返回冲突且不能继续更新任何记录。Duplicate labels return conflict without updating records. */
    @Test
    void rejectsDuplicateLabelWithoutWriting() throws Exception {
        when(mapper.countOtherLabels("fixture label", "fixture-value", "DATASET_TYPE")).thenReturn(1);
        mvc.perform(put("/api/smp/dataset-config/datasetTypes/fixture-value")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"fixture label\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
        verify(mapper, never()).updateDatasetConfigLabel(anyString(), anyString(), anyString());
    }

    /** 零行更新表示记录缺失，不能报告成功。A zero-row update means the record is missing and cannot report success. */
    @Test
    void returnsNotFoundWhenNoRecordWasUpdated() throws Exception {
        when(mapper.updateDatasetConfigLabel("fixture label", "fixture-value", "DATASET_TYPE")).thenReturn(0);
        mvc.perform(put("/api/smp/dataset-config/datasetTypes/fixture-value")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"fixture label\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        verify(mapper).updateDatasetConfigLabel("fixture label", "fixture-value", "DATASET_TYPE");
    }
}
