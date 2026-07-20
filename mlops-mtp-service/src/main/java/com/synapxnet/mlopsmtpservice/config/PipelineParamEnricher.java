package com.synapxnet.mlopsmtpservice.config;

import com.synapxnet.mlopsmtpservice.entity.*;
import com.synapxnet.mlopsmtpservice.service.AlgorithmService;
import com.synapxnet.mlopsmtpservice.service.TrainTaskService;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.stream.Collectors;

@Component
public class PipelineParamEnricher {

    private final AlgorithmService algorithmService;
    private final TrainTaskService trainTaskService;

    public PipelineParamEnricher(AlgorithmService algorithmService,TrainTaskService trainTaskService) {
        this.algorithmService = algorithmService;
        this.trainTaskService = trainTaskService;
    }

    public void enrichParamsWithTaskInfo(PipelineConfigParams params, TrainTask task,String taskUID) {
        // 1. 数据集相关
        // hdfs 文件
        if (task.getDatasets() != null && !task.getDatasets().isEmpty()) {

            // 将所有数据集ID用逗号连接作为HDFS路径
            String hdfsPaths = task.getDatasets().stream()
                    .map(TaskDataset::getDataset_uid)
                    .collect(Collectors.joining(","));
            params.setDatasetFolder( task.getDatasets().get(0).getBucket_identifier() +"/"+ hdfsPaths);

            // 本地路径可以保持不变或做类似处理
            params.setDatasetPath(taskUID+"/"+ hdfsPaths);
        } else {
            params.setDatasetFolder("");
            params.setDatasetPath("");
        }

        // 2. 代码仓库相关
        String algorithmUid = task.getAlgorithm_uid();
        Algorithm algorithm = null;

        AlgorithmRepository algorithmRepository = null;
        if (algorithmUid != null && !algorithmUid.isEmpty()) {
            algorithm = algorithmService.getAlgorithmByUID(algorithmUid);
            algorithmRepository = algorithmService.getAlgorithmByUid(algorithm.getCloud_algorithm_id());
        }

        // 仓库ID
        params.setGitRepoId("Synap-Xnet");
        params.setGitRepoUrl(algorithmRepository.getUrl());
        byte[] encryptedToken = algorithmRepository.getEncrypted_token();
        if (encryptedToken != null) {

            String base64Token = Base64.getEncoder().encodeToString(encryptedToken);

            params.setGitRepoPassword(base64Token);

        } else {
            params.setGitRepoPassword(""); // 避免空指针
        }


        // Jenkins凭证ID (使用固定值，根据要求)
        params.setCredentialsId("Synap-Xnet");
        params.setGitBranch(algorithmRepository.getAlgorithm_version());

        // 3. 训练相关
        params.setPythonEntryPoint(task.getTask_route());  // 任务路由作为Python入口
        params.setContainerName(task.getUid());          // 镜像名称作为容器名称
        String outputConfig = task.getOutput_config();
        JSONObject json = new JSONObject(outputConfig);
        String outputPath = json.getString("outputPath");
        params.setModelOutputPath(outputPath);// 输出配置作为模型输出路径

        // 4. 清理相关
        params.setContainerToRemove(task.getImage());      // 使用镜像名称作为要删除的容器

        // 5. 算法信息
        params.setAlgorithmName(task.getAlgorithm_name());
        params.setAlgorithmVersion(task.getAlgorithm_version());

        // 6.拉取docker容器
        String image_uid = task.getImage_uid();
        DockerFile dockerFile = trainTaskService.findImageByUid(image_uid)
                .orElseThrow(() -> new RuntimeException("镜像不存在: " + image_uid));
        params.setDockerImageName(dockerFile.getName());
        params.setDockerImageTags(dockerFile.getTags());
        //harbor Jenkins凭证ID
        params.setHarborCredentialsId("Synap-Xnet-Harbor");
        String harbor_uid = dockerFile.getHarbor_uid();
        HarborRepository harborRepository = trainTaskService.findByHarborUid(harbor_uid)
                .orElseThrow(() -> new RuntimeException("仓库不存在: " + harbor_uid));

        //harborurl
        params.setHarborUrl("http://"+harborRepository.getUrl()+":80");





        // 6. 设置默认值防止空指针
        if (params.getContainerName() == null || params.getContainerName().isEmpty()) {
            params.setContainerName("training-container");
        }
        if (params.getContainerToRemove() == null || params.getContainerToRemove().isEmpty()) {
            params.setContainerToRemove("training-container");
        }
    }
}