package com.synapxnet.mlopsxaaservice.dto;

import lombok.Data;

import java.util.List;

/**
 * OpenXnet 企业 Skill 候选导入请求。
 */
@Data
public class OpenXnetSkillImportRequest {
    private String workspaceId;
    private String skillId;
    private String familyId;
    private String name;
    private String description;
    private String version;
    private String contentMd;
    private String manifestJson;
    private String artifactDigest;
    private String lifecycleStatus;
    private String evidenceOrigin;
    private String environmentScope;
    private Boolean productionEligible;
    private List<String> files;
}
