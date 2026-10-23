/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 密钥编辑持久化 / API key metadata persistence.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.service;

import com.synapxnet.mlopsmepservice.entity.ApiKey;
import com.synapxnet.mlopsmepservice.mapper.ApiKeyMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyMetadataUpdateTest {
    /** 清空限制不得重新启用停用密钥。 Clearing limits must not reactivate a disabled key. */
    @Test void editingLimitsPreservesDisabledState() {
        ApiKeyMapper mapper = mock(ApiKeyMapper.class);
        ApiKey existing = new ApiKey(); existing.setId(3L); existing.setName("key");
        existing.setStatus("disabled"); existing.setUsageLimit(10L);
        existing.setExpiresAt(LocalDateTime.of(2026, 12, 31, 0, 0));
        when(mapper.findById(3L)).thenReturn(existing);
        ApiKey edit = new ApiKey(); edit.setId(3L); edit.setName("renamed"); edit.setStatus("active");
        new ApiKeyService(mapper).update(edit);
        verify(mapper).update(argThat(saved -> saved.getStatus().equals("disabled")
            && saved.getName().equals("renamed") && saved.getUsageLimit() == null
            && saved.getExpiresAt() == null));
    }

    /** 不存在的密钥不能返回更新成功。 Missing keys must not report successful edits. */
    @Test void missingKeyRejectsUpdate() {
        ApiKeyMapper mapper = mock(ApiKeyMapper.class);
        ApiKey edit = new ApiKey(); edit.setId(9L);
        assertThrows(IllegalArgumentException.class, () -> new ApiKeyService(mapper).update(edit));
        verify(mapper, never()).update(any());
    }
}
