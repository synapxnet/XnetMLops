package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;

@Data
public class HdfsFile {
    private String id;
    private String name;
    private String path;
    private boolean isDirectory;
    private long size;
    private long modificationTime;
    private String permissions;
    private String owner;
    private String group;
}
