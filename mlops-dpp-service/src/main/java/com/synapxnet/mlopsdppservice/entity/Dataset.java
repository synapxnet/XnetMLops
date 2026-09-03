package com.synapxnet.mlopsdppservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.util.Date;

@Data
public class Dataset {
    private Long id;
    private String uid;
    private String userId;
    private String dataset_file;
    private String type;
    private String type_label;
    private String zone;
    private String zone_label;
    private Boolean encryption;
    private String subdata_area;
    private String bucket_name;
    private String bucket_identifier;
    private String tenant_uid;
    private String dept_uid;
    private String team_uid;
    private String team_name;
    private String description;
    private String sourcePlatform;
    private String sourceProductName;
    private String sourceProductVersion;
    private String sourceUri;
    private Long rowCount;
    private Long byteSize;
    private String schemaDigestSha256;
    private String artifactDigestSha256;
    private String lineageReference;
    private String importStatus;
    private Date importedAt;
    private Date created_at;
    private Date updated_at;
    private int level;

    @Transient //临时存储路径
    private String tempFilePath;

}
