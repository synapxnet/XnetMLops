/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 *
 * HDFS 流式下载回归测试 — 验证有界读取、路径边界与异常释放。
 * HDFS streaming download regression tests — bounded reads, path boundaries and cleanup.
 * Author: maoyo
 * Department: 研发部
 * Date: 2026-09-15
 * Version: 1.3.0
 * Security Level: INTERNAL
 * Maintainer: maoyo
 * Email: synapxnet@gmail.com
 */
package com.synapxnet.mlops.storage;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ResourceHttpMessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HdfsFileDownloadTests {
    private static final URI ENDPOINT = URI.create("hdfs://hadoop-master:9000");
    private static final String ROOT = "/datasets/recommendation-curated/DS-TEST/version-1";
    private static final int BLOCK = 16 * 1024;

    /** 中文：大文件通过真实转换器输出而不全量缓冲；English: Stream a large generated file through the real converter without buffering it. */
    @Test
    void streamsLargeFileWithBoundedReadsAndExactLength() throws Exception {
        long length = 256L * 1024 * 1024 + 17;
        GeneratedInput source = new GeneratedInput(length);
        FileSystem fs = fileSystem(source, length);
        DiscardingMessage output = new DiscardingMessage(Long.MAX_VALUE);

        try (HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, "sample.bin")) {
            assertTrue(source.position <= BLOCK, "Opening a download must not preload the whole file");
            ResponseEntity<InputStreamResource> response = download.response();
            assertNotNull(response.getBody());
            assertEquals(InputStreamResource.class, response.getBody().getClass(), "A subclass unexpectedly enables Spring range handling");
            assertEquals(length, response.getHeaders().getContentLength());
            assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
            write(response, output);
            assertEquals(length, output.written);
            assertTrue(source.maxRequested <= BLOCK, "HDFS reads must remain bounded");
            assertTrue(source.closed, "The real converter must release the HDFS input");
            System.out.println("HDFS download proof: fileBytes=" + length
                    + ", maxHeapBytes=" + Runtime.getRuntime().maxMemory()
                    + ", maxReadBytes=" + source.maxRequested + ", deliveredBytes=" + output.written);
        }
        verify(fs, times(1)).close();
    }

    /** 中文：未消费的预读流也能幂等释放；English: An unconsumed preflight stream can be closed idempotently. */
    @Test
    void closesUnusedDownloadAndFileSystemExactlyOnce() throws Exception {
        GeneratedInput source = new GeneratedInput(100_000);
        FileSystem fs = fileSystem(source, 100_000);
        HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, "sample.bin");
        download.close();
        download.close();
        assertTrue(source.closed);
        assertEquals(1, source.closeCount);
        verify(fs, times(1)).close();
    }

    /** 中文：数据流关闭失败也不能泄漏连接或重复关闭；English: A source close failure must not leak the filesystem or cause duplicate closes. */
    @Test
    void sourceCloseFailureStillClosesFileSystemIdempotently() throws Exception {
        GeneratedInput source = new GeneratedInput(100_000);
        source.closeFailure = new IOException("test source close failure");
        FileSystem fs = fileSystem(source, 100_000);
        HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, "sample.bin");
        assertThrows(IOException.class, download::close);
        assertDoesNotThrow(download::close);
        assertTrue(source.closed);
        assertEquals(1, source.closeCount);
        verify(fs, times(1)).close();
    }

    /** 中文：空文件有零长度且正常关闭；English: A zero-byte file has explicit length and releases its resources. */
    @Test
    void streamsEmptyFileWithoutInventingBytes() throws Exception {
        GeneratedInput source = new GeneratedInput(0);
        FileSystem fs = fileSystem(source, 0);
        DiscardingMessage output = new DiscardingMessage(Long.MAX_VALUE);
        try (HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, "empty.bin")) {
            assertEquals(0, download.response().getHeaders().getContentLength());
            write(download.response(), output);
            assertEquals(0, output.written);
        }
        assertTrue(source.closed);
        verify(fs, times(1)).close();
    }

    /** 中文：文件不存在时在发送响应前失败并关闭连接；English: A missing file fails before response creation and closes the owned filesystem. */
    @Test
    void missingFileFailsBeforeOpeningDataStream() throws Exception {
        FileSystem fs = fileSystem(new GeneratedInput(0), 0);
        when(fs.getFileStatus(any(Path.class))).thenThrow(new FileNotFoundException("test missing file"));
        assertThrows(IOException.class, () -> HdfsFileDownload.open(fs, ROOT, "missing.bin"));
        verify(fs, never()).open(any(Path.class), anyInt());
        verify(fs, times(1)).close();
    }

    /** 中文：目录不能伪装成可下载文件；English: A directory must not be exposed as a downloadable file. */
    @Test
    void directoryFailsBeforeOpeningDataStream() throws Exception {
        FileSystem fs = fileSystem(new GeneratedInput(0), 0);
        when(fs.getFileStatus(any(Path.class))).thenReturn(new FileStatus(0, true, 1, BLOCK, 0, new Path(ROOT + "/directory")));
        assertThrows(IOException.class, () -> HdfsFileDownload.open(fs, ROOT, "directory"));
        verify(fs, never()).open(any(Path.class), anyInt());
        verify(fs, times(1)).close();
    }

    /** 中文：首块读取错误在响应前可见；English: A first-block read failure is visible before a successful response exists. */
    @Test
    void firstReadFailureClosesInputAndFileSystem() throws Exception {
        GeneratedInput source = new GeneratedInput(100_000, 0, new IOException("test first read failure"));
        FileSystem fs = fileSystem(source, 100_000);
        assertThrows(IOException.class, () -> HdfsFileDownload.open(fs, ROOT, "sample.bin"));
        assertTrue(source.closed);
        verify(fs, times(1)).close();
    }

    /** 中文：预读阶段提前结束不能产生成功响应；English: Premature EOF during preflight cannot produce a success response. */
    @Test
    void shortPreflightFailsBeforeResponse() throws Exception {
        GeneratedInput source = new GeneratedInput(37);
        FileSystem fs = fileSystem(source, BLOCK * 3L);
        assertThrows(IOException.class, () -> HdfsFileDownload.open(fs, ROOT, "sample.bin"));
        assertTrue(source.closed);
        verify(fs, times(1)).close();
    }

    /** 中文：响应开始后提前结束必须抛错而非成功；English: Premature EOF after response start must propagate as failure. */
    @Test
    void midStreamEofIsNotReportedAsCompleted() throws Exception {
        long advertised = BLOCK * 4L;
        GeneratedInput source = new GeneratedInput(BLOCK * 2L + 7);
        FileSystem fs = fileSystem(source, advertised);
        DiscardingMessage output = new DiscardingMessage(Long.MAX_VALUE);
        try (HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, "sample.bin")) {
            IOException failure = assertThrows(IOException.class, () -> write(download.response(), output));
            assertFalse(failure instanceof FileNotFoundException, "Spring silently swallows FileNotFoundException during writing");
            assertTrue(output.written < advertised);
            assertTrue(source.closed);
        }
        verify(fs, times(1)).close();
    }

    /** 中文：中途丢失文件不能被 Spring 吞掉；English: A mid-stream missing-file exception must not be swallowed by Spring. */
    @Test
    void midStreamFileNotFoundRemainsVisibleThroughSpringConverter() throws Exception {
        long length = BLOCK * 5L;
        GeneratedInput source = new GeneratedInput(length, BLOCK * 2L, new FileNotFoundException("test vanished block"));
        FileSystem fs = fileSystem(source, length);
        DiscardingMessage output = new DiscardingMessage(Long.MAX_VALUE);
        try (HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, "sample.bin")) {
            IOException failure = assertThrows(IOException.class, () -> write(download.response(), output));
            assertFalse(failure instanceof FileNotFoundException);
            assertTrue(output.written < length);
            assertTrue(source.closed);
        }
        verify(fs, times(1)).close();
    }

    /** 中文：客户端取消后立即释放 HDFS 流和连接；English: A client abort releases the HDFS input and connection. */
    @Test
    void clientAbortClosesResourcesWithoutFinishingTheFile() throws Exception {
        long length = BLOCK * 20L;
        GeneratedInput source = new GeneratedInput(length);
        FileSystem fs = fileSystem(source, length);
        DiscardingMessage output = new DiscardingMessage(BLOCK + 3L);
        try (HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, "sample.bin")) {
            assertThrows(IOException.class, () -> write(download.response(), output));
            assertTrue(source.position < length);
            assertTrue(source.closed);
        }
        verify(fs, times(1)).close();
    }

    /** 中文：同 HDFS 的完整 URI 与中文文件名保持正确；English: A qualified URI on the same filesystem preserves its Unicode filename. */
    @Test
    void acceptsQualifiedPathAndPreservesUnicodeDownloadName() throws Exception {
        GeneratedInput source = new GeneratedInput(11);
        FileSystem fs = fileSystem(source, 11);
        String name = "模型 指标.csv";
        try (HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, ENDPOINT + ROOT + "/" + name)) {
            assertEquals(name, download.response().getHeaders().getContentDisposition().getFilename());
            verify(fs).open(argThat((Path path) -> (ROOT + "/" + name).equals(path.toUri().getPath())), eq(BLOCK));
        }
        verify(fs, times(1)).close();
    }

    /** 中文：原生完整路径与相对路径均能下载；English: Both the native full path and a relative file path remain supported. */
    @Test
    void acceptsNativeRootedPath() throws Exception {
        GeneratedInput source = new GeneratedInput(11);
        FileSystem fs = fileSystem(source, 11);
        try (HdfsFileDownload download = HdfsFileDownload.open(fs, ROOT, ROOT + "/nested/sample.bin")) {
            assertNotNull(download.response().getBody());
            verify(fs).open(argThat((Path path) -> (ROOT + "/nested/sample.bin").equals(path.toUri().getPath())), eq(BLOCK));
        }
    }

    /** 中文：拒绝目录穿越、邻接根目录和外部文件系统；English: Reject traversal, sibling roots and foreign filesystems. */
    @Test
    void rejectsCrossRootAndForeignQualifiedPathsBeforeReading() throws Exception {
        for (String request : new String[] {
                "../other/sample.bin",
                ROOT + "/../other/sample.bin",
                ROOT + "-sibling/sample.bin",
                "hdfs://foreign-host:9000" + ROOT + "/sample.bin",
                "file:///etc/passwd",
                "hdfs://hadoop-master:9000/datasets/other/sample.bin"
        }) {
            FileSystem fs = fileSystem(new GeneratedInput(100), 100);
            assertThrows(Exception.class, () -> HdfsFileDownload.open(fs, ROOT, request), request);
            verify(fs, never()).open(any(Path.class), anyInt());
            verify(fs, times(1)).close();
        }
    }

    /** 中文：解析后的链接也必须留在授权根目录；English: A resolved symbolic-link target must remain inside the allowed root. */
    @Test
    void rejectsResolvedPathOutsideRootBeforeReading() throws Exception {
        FileSystem fs = fileSystem(new GeneratedInput(100), 100);
        when(fs.resolvePath(any(Path.class))).thenReturn(new Path(ENDPOINT + "/datasets/other/secret.bin"));
        assertThrows(Exception.class, () -> HdfsFileDownload.open(fs, ROOT, "link.bin"));
        verify(fs, never()).open(any(Path.class), anyInt());
        verify(fs, times(1)).close();
    }

    /** 中文：构造不连接网络的 Hadoop 文件系统替身；English: Build a mocked Hadoop filesystem without network access. */
    private static FileSystem fileSystem(GeneratedInput input, long declaredLength) throws IOException {
        FileSystem fs = mock(FileSystem.class);
        when(fs.getUri()).thenReturn(ENDPOINT);
        when(fs.getWorkingDirectory()).thenReturn(new Path("/"));
        when(fs.makeQualified(any(Path.class))).thenAnswer(invocation -> ((Path) invocation.getArgument(0)).makeQualified(ENDPOINT, new Path("/")));
        when(fs.resolvePath(any(Path.class))).thenAnswer(invocation -> ((Path) invocation.getArgument(0)).makeQualified(ENDPOINT, new Path("/")));
        when(fs.getFileStatus(any(Path.class))).thenAnswer(invocation -> new FileStatus(declaredLength, false, 1, 128L * 1024 * 1024, 0, invocation.getArgument(0)));
        when(fs.open(any(Path.class), anyInt())).thenReturn(new FSDataInputStream(input));
        return fs;
    }

    /** 中文：使用生产 Spring 转换器写出响应；English: Exercise the actual production Spring resource converter. */
    private static void write(ResponseEntity<InputStreamResource> response, DiscardingMessage output) throws IOException {
        output.getHeaders().putAll(response.getHeaders());
        new ResourceHttpMessageConverter().write(response.getBody(), response.getHeaders().getContentType(), output);
    }

    private static final class GeneratedInput extends FSInputStream {
        private final long length;
        private final long failAt;
        private final IOException readFailure;
        private long position;
        private int maxRequested;
        private boolean closed;
        private int closeCount;
        private IOException closeFailure;

        /** 中文：创建固定长度的生成流；English: Create a generated stream with a fixed logical size. */
        private GeneratedInput(long length) {
            this(length, Long.MAX_VALUE, null);
        }

        /** 中文：创建可在指定位置故障的流；English: Create a stream that can fail at a specific offset. */
        private GeneratedInput(long length, long failAt, IOException readFailure) {
            this.length = length;
            this.failAt = failAt;
            this.readFailure = readFailure;
        }

        /** 中文：读取单字节并遵守相同故障边界；English: Read one byte using the same failure boundary. */
        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) == -1 ? -1 : one[0] & 0xff;
        }

        /** 中文：按需生成内容并记录最大读取量；English: Generate bytes on demand and record the largest read request. */
        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            if (closed) throw new IOException("test input already closed");
            if (count == 0) return 0;
            maxRequested = Math.max(maxRequested, count);
            if (readFailure != null && position >= failAt) throw readFailure;
            if (position >= length) return -1;
            long remaining = Math.min(length - position, failAt - position);
            int actual = (int) Math.min(count, remaining);
            Arrays.fill(buffer, offset, offset + actual, (byte) 0x5a);
            position += actual;
            return actual;
        }

        /** 中文：提供 Hadoop 所需定位接口；English: Provide the seek interface required by Hadoop. */
        @Override
        public void seek(long target) throws IOException {
            if (target < 0 || target > length) throw new EOFException("test invalid seek");
            position = target;
        }

        /** 中文：返回当前位置；English: Return the current logical position. */
        @Override
        public long getPos() {
            return position;
        }

        /** 中文：测试流不切换数据源；English: This test stream never switches its data source. */
        @Override
        public boolean seekToNewSource(long target) {
            return false;
        }

        /** 中文：记录关闭次数以验证幂等释放；English: Record closes to verify idempotent ownership cleanup. */
        @Override
        public void close() throws IOException {
            closed = true;
            closeCount++;
            if (closeFailure != null) throw closeFailure;
        }
    }

    private static final class DiscardingMessage implements HttpOutputMessage {
        private final HttpHeaders headers = new HttpHeaders();
        private final long abortAt;
        private long written;

        /** 中文：构造只计数、不保留文件内容的输出；English: Create an output that counts bytes without retaining file contents. */
        private DiscardingMessage(long abortAt) {
            this.abortAt = abortAt;
        }

        /** 中文：返回响应头；English: Return response headers. */
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        /** 中文：提供可模拟客户端取消的丢弃流；English: Provide a discarding output that can simulate client cancellation. */
        @Override
        public OutputStream getBody() {
            return new OutputStream() {
                /** 中文：计数单字节；English: Count a single output byte. */
                @Override
                public void write(int value) throws IOException {
                    if (written >= abortAt) throw new IOException("test client disconnected");
                    written++;
                }

                /** 中文：计数块输出并在边界处模拟取消；English: Count a block and simulate cancellation at the configured boundary. */
                @Override
                public void write(byte[] buffer, int offset, int count) throws IOException {
                    if (count > abortAt - written) {
                        written = abortAt;
                        throw new IOException("test client disconnected");
                    }
                    written += count;
                }
            };
        }
    }
}
