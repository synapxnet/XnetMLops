/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * Synapxnet Proprietary and Confidential. Copying, distribution or use requires explicit authorization.
 * 存储桶元数据编辑回归 / Bucket metadata edit regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-10-23 | Version: 1.0.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.Utils.HadoopUtil;
import com.synapxnet.mlopssmpservice.entity.Bucket;
import com.synapxnet.mlopssmpservice.mapper.BucketMapper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BucketMetadataUpdateTests {
    /** 配置修改不能覆盖采集用量或清空隐藏凭据。 Configuration edits preserve measured usage and hidden credentials. */
    @Test void editingKeepsMeasuredUsageAndWriteOnlySecret() {
        BucketMapper mapper = mock(BucketMapper.class);
        Bucket current = new Bucket(); current.setId(1L); current.setCurrent_size(12.5); current.setMax_size(80.0); current.setAccess_key("fixture-secret");
        when(mapper.selectBucketById(1L)).thenReturn(current);
        Bucket changes = new Bucket(); changes.setName("New name"); changes.setCurrent_size(0.0); changes.setAccess_key("");
        Bucket saved = new BucketService(mapper, mock(HadoopUtil.class)).updateBucket(1L, changes);
        assertEquals(12.5, saved.getCurrent_size());
        assertEquals(80.0, saved.getMax_size());
        assertEquals("fixture-secret", saved.getAccess_key());
        assertEquals("New name", saved.getName());
        verify(mapper).updateBucket(current);
    }

    /** 前端新增的零ID必须走创建与真实存储步骤。 A zero draft identifier must invoke creation and the storage boundary. */
    @Test void draftZeroIdentifierCreatesInsteadOfUpdating() throws Exception {
        BucketMapper mapper = mock(BucketMapper.class);
        HadoopUtil hadoop = mock(HadoopUtil.class);
        FileSystem filesystem = mock(FileSystem.class);
        when(hadoop.getFileSystem()).thenReturn(filesystem);
        when(filesystem.mkdirs(any(Path.class))).thenReturn(true);
        doAnswer(invocation -> { Bucket inserted = invocation.getArgument(0); assertNull(inserted.getId()); inserted.setId(7L); return 1; })
                .when(mapper).insertBucket(any(Bucket.class));
        Bucket draft = new Bucket(); draft.setId(0L); draft.setIdentifier("test-bucket");
        when(mapper.selectBucketById(7L)).thenReturn(draft);
        BucketService service = new BucketService(mapper, hadoop);
        ReflectionTestUtils.setField(service, "hdfsBasePath", "/buckets");
        service.saveBucket(draft);
        verify(mapper).insertBucket(draft);
        verify(mapper, never()).updateBucket(any(Bucket.class));
        verify(filesystem).mkdirs(new Path("/buckets/test-bucket"));
    }
}
