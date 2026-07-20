package com.synapxnet.mlopssmpservice.entity;

import lombok.Data;

@Data
public class PipelineConfigParams {
    private String dockerImageName;
    private String dockerImageTags;  // 逗号分隔的标签列表
    private String harborUrl;
    private String dockerfileContent;
    private String harborCredentialsId; // Jenkins 中存储的 Harbor 凭证 ID

    private String description;
}
