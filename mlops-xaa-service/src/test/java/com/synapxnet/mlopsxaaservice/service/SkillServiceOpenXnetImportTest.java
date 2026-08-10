package com.synapxnet.mlopsxaaservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.mlopsxaaservice.dto.OpenXnetSkillImportRequest;
import com.synapxnet.mlopsxaaservice.entity.Skill;
import com.synapxnet.mlopsxaaservice.mapper.SkillCategoryMapper;
import com.synapxnet.mlopsxaaservice.mapper.SkillInstallationMapper;
import com.synapxnet.mlopsxaaservice.mapper.SkillMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OpenXnet 企业 Skill 候选入库规则测试。
 */
class SkillServiceOpenXnetImportTest {

    /**
     * 验证首次导入只创建草稿且不会产生安装状态。
     */
    @Test
    void importsCandidateAsDraft() {
        SkillMapper skillMapper = mock(SkillMapper.class);
        SkillService service = createService(skillMapper);
        OpenXnetSkillImportRequest request = createRequest();
        when(skillMapper.selectByUid(anyString())).thenReturn(null);

        Skill result = service.importOpenXnetSkill(request, "17870171303");

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillMapper).insert(captor.capture());
        assertEquals("draft", result.getStatus());
        assertEquals("ws_goai_demo", result.getTenantUid());
        assertEquals("17870171303", result.getCreatorId());
        assertFalse(Boolean.TRUE.equals(result.getIsOfficial()));
        assertEquals("draft", captor.getValue().getStatus());
    }

    /**
     * 验证相同 Workspace、Skill 与摘要的重复导入保持幂等。
     */
    @Test
    void keepsSameDigestIdempotent() {
        SkillMapper skillMapper = mock(SkillMapper.class);
        SkillService service = createService(skillMapper);
        OpenXnetSkillImportRequest request = createRequest();
        Skill existing = new Skill();
        existing.setId(7L);
        existing.setUid("OPENXNET-EXISTING");
        existing.setTenantUid(request.getWorkspaceId());
        existing.setStatus("draft");
        existing.setConfigJson("{\"artifact_digest\":\"" + request.getArtifactDigest() + "\"}");
        when(skillMapper.selectByUid(anyString())).thenReturn(existing);

        Skill result = service.importOpenXnetSkill(request, "17870171303");

        assertEquals(7L, result.getId());
        verify(skillMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(skillMapper, never()).update(org.mockito.ArgumentMatchers.any());
    }

    /**
     * 创建仅依赖 Mapper Mock 的服务实例。
     *
     * @param skillMapper Skill Mapper Mock
     * @return Skill 服务
     */
    private SkillService createService(SkillMapper skillMapper) {
        return new SkillService(
                skillMapper,
                mock(SkillCategoryMapper.class),
                mock(SkillInstallationMapper.class),
                new ObjectMapper());
    }

    /**
     * 创建摘要与内容一致的最小候选包。
     *
     * @return OpenXnet Skill 导入请求
     */
    private OpenXnetSkillImportRequest createRequest() {
        OpenXnetSkillImportRequest request = new OpenXnetSkillImportRequest();
        request.setWorkspaceId("ws_goai_demo");
        request.setSkillId("goai-evidence-collect");
        request.setFamilyId("goai-evidence-collect");
        request.setName("证据采集");
        request.setDescription("只读采集跨平台证据");
        request.setVersion("1.0.0");
        request.setContentMd("# Skill\n");
        request.setManifestJson("{}");
        request.setArtifactDigest(createDigest(request.getContentMd(), request.getManifestJson()));
        request.setLifecycleStatus("candidate");
        request.setEvidenceOrigin("rehearsal");
        request.setEnvironmentScope("simulation");
        request.setProductionEligible(false);
        request.setFiles(List.of("SKILL.md", "openxnet.skill.json"));
        return request;
    }

    /**
     * 按生产契约计算测试制品摘要。
     *
     * @param contentMd SKILL.md 内容
     * @param manifestJson 扩展清单
     * @return SHA-256 小写摘要
     */
    private String createDigest(String contentMd, String manifestJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(contentMd.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(manifestJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
