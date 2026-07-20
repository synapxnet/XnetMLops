package com.synapxnet.mlopssmpservice.config;

import com.synapxnet.mlopssmpservice.entity.*;
import org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import com.synapxnet.mlopssmpservice.service.*;
import java.util.Base64;
import java.util.stream.Collectors;

@Component
public class PipelineParamEnricher {

//    private final AlgorithmService algorithmService;
//
//    public PipelineParamEnricher(AlgorithmService algorithmService) {
//        this.algorithmService = algorithmService;
//    }


    private final HarborRepositoryService harborRepositoryService;
    public PipelineParamEnricher(HarborRepositoryService harborRepositoryService) {
        this.harborRepositoryService=harborRepositoryService;
    }
    // 为Docker构建任务丰富参数
    public void enrichParamsForDockerBuild(PipelineConfigParams params, DockerFile dockerFile,String dockerBuild) {
        // 1. 设置Docker构建参数
        params.setDockerImageName(dockerFile.getName());

        // 处理标签：将'-'分隔的标签转换为逗号分隔
        if (dockerFile.getTags() != null) {
            String tags = dockerFile.getTags().replace("-", ",");
            params.setDockerImageTags(tags);
        } else {
            params.setDockerImageTags("latest");
        }
        String harbor_uid = dockerFile.getHarbor_uid();
        HarborRepository harborRepository = harborRepositoryService.getRepositoryByUid(harbor_uid)
                .orElseThrow(() -> new ResourceNotFoundException("harbor not found with UID: " + harbor_uid));


        params.setHarborUrl("http://"+harborRepository.getUrl()+":80");
        params.setDockerfileContent(dockerFile.getContent());
        params.setHarborCredentialsId("Synap-Xnet-Harbor");

        // 2. 设置可选描述
        params.setDescription("Docker镜像构建流水线");


    }
}
