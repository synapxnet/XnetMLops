/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 有界内存、独立连接的 HDFS 下载。 / Bounded-memory HDFS downloads with an owned connection.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-15 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlops.storage;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;

/** 每次下载独占文件系统，资源转换器负责最终关闭。 / Each download owns its filesystem until the resource converter closes it. */
public final class HdfsFileDownload implements AutoCloseable {
    static final int BUFFER_SIZE = 16 * 1024;

    private final OwnedInputStream stream;
    private final long length;
    private final String filename;

    /** 保存预检查通过的流和固定响应元数据。 / Keep the preflighted stream and fixed response metadata. */
    private HdfsFileDownload(OwnedInputStream stream, long length, String filename) {
        this.stream = stream;
        this.length = length;
        this.filename = filename;
    }

    /** 为下载创建独立连接，不关闭列表或写入使用的共享连接。 / Create a fresh download client without closing clients used by lists or writes. */
    public static HdfsFileDownload open(String root, String requested, String endpoint, String username) throws Exception {
        Configuration configuration = new Configuration();
        configuration.set("fs.defaultFS", endpoint);
        configuration.set("dfs.client.use.datanode.hostname", "true");
        configuration.setIfUnset("dfs.client.socket-timeout", "15000");
        configuration.setIfUnset("dfs.client.retry.window.base", "500");
        configuration.setIfUnset("dfs.client.max.block.acquire.failures", "2");
        URI filesystemUri = new URI(endpoint);
        resolve(root, requested, filesystemUri);
        if ("kerberos".equalsIgnoreCase(configuration.get("hadoop.security.authentication"))) {
            UserGroupInformation.setConfiguration(configuration);
            UserGroupInformation.loginUserFromKeytab(
                    configuration.get("kerberos.principal"), configuration.get("kerberos.keytab"));
        }
        try {
            FileSystem filesystem = UserGroupInformation.createRemoteUser(username)
                    .doAs((PrivilegedExceptionAction<FileSystem>) () -> FileSystem.newInstance(filesystemUri, configuration));
            return open(filesystem, root, requested);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw error;
        }
    }

    /** 接管独立连接并预读首块，失败时释放所有已打开资源。 / Own the isolated connection and prefetch the first block, releasing resources on failure. */
    static HdfsFileDownload open(FileSystem filesystem, String root, String requested) throws IOException {
        FSDataInputStream input = null;
        try {
            Path path = resolve(root, requested, filesystem.getUri());
            Path resolved = filesystem.resolvePath(path);
            requireContained(root, resolved, filesystem.getUri());
            FileStatus status = filesystem.getFileStatus(resolved);
            if (!status.isFile()) {
                throw new FileNotFoundException("Download target is not a regular file");
            }
            long length = status.getLen();
            if (length < 0) {
                throw new IOException("Invalid download length");
            }
            input = filesystem.open(resolved, BUFFER_SIZE);
            OwnedInputStream owned = new OwnedInputStream(input, filesystem, length);
            owned.prefetch();
            return new HdfsFileDownload(owned, length, resolved.getName());
        } catch (IOException | RuntimeException error) {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException closeError) {
                    error.addSuppressed(closeError);
                }
            }
            try {
                filesystem.close();
            } catch (IOException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
    }

    /** 校验协议和根边界，同时兼容列表限定路径与旧相对路径。 / Validate scheme and root containment while accepting qualified and legacy relative paths. */
    static Path resolve(String root, String requested, URI filesystemUri) {
        if (requested == null || requested.isBlank() || requested.indexOf('\\') >= 0
                || requested.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid download path");
        }
        Path rootPath = new Path(root);
        URI rootUri = rootPath.toUri();
        String rootName = rootUri.normalize().getPath();
        if (!rootPath.isAbsolute() || rootUri.getScheme() != null || rootUri.getAuthority() != null
                || rootName == null || "/".equals(rootName) || root.contains("..")) {
            throw new IllegalArgumentException("Invalid download root");
        }
        while (rootName.endsWith("/")) {
            rootName = rootName.substring(0, rootName.length() - 1);
        }
        Path supplied = new Path(requested);
        URI suppliedUri = supplied.toUri();
        String suppliedName = suppliedUri.getPath();
        boolean qualified = suppliedUri.getScheme() != null || suppliedUri.getAuthority() != null;
        if (qualified) {
            requireFilesystem(suppliedUri, filesystemUri);
        }
        String candidate;
        if (qualified || suppliedName.equals(rootName) || suppliedName.startsWith(rootName + "/")) {
            candidate = suppliedName;
        } else {
            int separator = rootName.indexOf('/', 1);
            String namespace = separator < 0 ? rootName + "/" : rootName.substring(0, separator + 1);
            if (suppliedName.startsWith(namespace)) {
                throw new IllegalArgumentException("Download path is outside the resource root");
            }
            candidate = rootName + "/" + (suppliedName.startsWith("/") ? suppliedName.substring(1) : suppliedName);
        }
        Path resolved = new Path(new Path(candidate).toUri().normalize());
        requireContained(rootName, resolved, filesystemUri);
        return resolved;
    }

    /** 拒绝跨文件系统、用户信息及非文件路径组成。 / Reject other filesystems, user information, and non-path URI components. */
    private static void requireFilesystem(URI candidate, URI filesystemUri) {
        if (candidate.getUserInfo() != null || candidate.getQuery() != null || candidate.getFragment() != null
                || !Objects.equals(candidate.getScheme(), filesystemUri.getScheme())
                || !Objects.equals(candidate.getAuthority(), filesystemUri.getAuthority())) {
            throw new IllegalArgumentException("Download filesystem does not match");
        }
    }

    /** 对最终解析路径再次检查目录边界，阻止符号链接逃逸。 / Recheck the resolved directory boundary, including symlink escapes. */
    private static void requireContained(String root, Path path, URI filesystemUri) {
        URI uri = path.toUri().normalize();
        if (uri.getScheme() != null || uri.getAuthority() != null) {
            requireFilesystem(uri, filesystemUri);
        }
        String rootName = new Path(root).toUri().normalize().getPath();
        while (rootName.endsWith("/")) {
            rootName = rootName.substring(0, rootName.length() - 1);
        }
        String name = uri.getPath();
        if (name == null || !name.startsWith(rootName + "/")) {
            throw new IllegalArgumentException("Download path is outside the resource root");
        }
    }

    /** 返回一次性流和准确长度，保留原下载名称并正确编码。 / Return a one-shot stream and exact length with the original safely encoded filename. */
    public ResponseEntity<InputStreamResource> response() throws IOException {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(length)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                    .body(new InputStreamResource(stream));
        } catch (RuntimeException error) {
            try {
                close();
            } catch (IOException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
    }

    /** 允许调用方在交给响应转换器前取消下载。 / Allow cancellation before handing the download to the response converter. */
    @Override
    public void close() throws IOException {
        stream.close();
    }

    /** 只保存首块，限制读取大小并拥有数据流和文件系统。 / Keep one initial block, bound reads, and own both the stream and filesystem. */
    private static final class OwnedInputStream extends InputStream {
        private final InputStream source;
        private final FileSystem filesystem;
        private final long expectedLength;
        private byte[] initial;
        private int initialPosition;
        private int initialLength;
        private long delivered;
        private boolean closed;

        /** 初始化固定大小首块，空文件不分配数据缓存。 / Allocate a bounded initial block, avoiding a data buffer for empty files. */
        private OwnedInputStream(InputStream source, FileSystem filesystem, long expectedLength) {
            this.source = source;
            this.filesystem = filesystem;
            this.expectedLength = expectedLength;
            this.initial = new byte[(int) Math.min(BUFFER_SIZE, expectedLength)];
        }

        /** 在 HTTP 成功响应前读取首块，提前发现数据节点读取失败。 / Read the first block before HTTP success to detect unavailable data nodes early. */
        private void prefetch() throws IOException {
            while (initialLength < initial.length) {
                int count = source.read(initial, initialLength, initial.length - initialLength);
                if (count < 0) {
                    throw new EOFException("Download ended before the recorded length");
                }
                if (count == 0) {
                    int value = source.read();
                    if (value < 0) {
                        throw new EOFException("Download ended before the recorded length");
                    }
                    initial[initialLength++] = (byte) value;
                } else {
                    initialLength += count;
                }
            }
        }

        /** 单字节读取也遵守声明长度及错误传播。 / Apply declared-length and error semantics to single-byte reads. */
        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            int count = read(single, 0, 1);
            return count < 0 ? -1 : single[0] & 0xff;
        }

        /** 以最多一个固定缓冲区读取，拒绝提前结束且不吞掉缺失文件错误。 / Read at most one bounded chunk, rejecting early EOF and unsuppressed missing-file errors. */
        @Override
        public int read(byte[] destination, int offset, int requestedLength) throws IOException {
            Objects.checkFromIndexSize(offset, requestedLength, destination.length);
            if (closed) {
                throw new IOException("Download stream is closed");
            }
            if (requestedLength == 0) {
                return 0;
            }
            if (delivered == expectedLength) {
                return -1;
            }
            int count;
            if (initialPosition < initialLength) {
                count = Math.min(requestedLength, initialLength - initialPosition);
                System.arraycopy(initial, initialPosition, destination, offset, count);
                initialPosition += count;
                if (initialPosition == initialLength) {
                    initial = null;
                }
            } else {
                int limit = (int) Math.min(Math.min(requestedLength, BUFFER_SIZE), expectedLength - delivered);
                try {
                    count = source.read(destination, offset, limit);
                    if (count == 0) {
                        int value = source.read();
                        if (value >= 0) {
                            destination[offset] = (byte) value;
                            count = 1;
                        } else {
                            count = -1;
                        }
                    }
                } catch (FileNotFoundException error) {
                    throw new IOException("Download source became unavailable", error);
                }
                if (count < 0) {
                    throw new EOFException("Download ended before the recorded length");
                }
            }
            delivered += count;
            return count;
        }

        /** 即使关闭数据流失败，也释放本次独立连接；重复关闭无副作用。 / Release the owned connection even if stream close fails; repeated close is harmless. */
        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            initial = null;
            IOException failure = null;
            try {
                source.close();
            } catch (IOException error) {
                failure = error;
            }
            try {
                filesystem.close();
            } catch (IOException error) {
                if (failure == null) {
                    failure = error;
                } else {
                    failure.addSuppressed(error);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
