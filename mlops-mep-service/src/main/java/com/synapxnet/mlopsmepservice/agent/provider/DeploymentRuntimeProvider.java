package com.synapxnet.mlopsmepservice.agent.provider;

import com.synapxnet.mlopsmepservice.agent.DeploymentRevision;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;

import java.time.Duration;

/**
 * 定义部署领域与具体 Docker/Kubernetes 执行环境之间的稳定端口。
 */
public interface DeploymentRuntimeProvider {

    /**
     * 判断 Provider 是否支持当前部署。
     *
     * @param deployment 部署领域记录
     * @return 是否支持
     */
    boolean supports(ModelDeployment deployment);

    /**
     * 检查运行时实际状态，不使用数据库 running 字段代替健康。
     *
     * @param deployment 部署领域记录
     * @return 运行时检查结果
     */
    RuntimeInspection inspect(ModelDeployment deployment);

    /**
     * 在产生远程副作用前验证修订镜像和部署规格。
     *
     * @param deployment 当前部署
     * @param revision 目标修订
     */
    void validateRevision(ModelDeployment deployment, DeploymentRevision revision);

    /**
     * 将真实运行时切换到目标修订；失败必须抛出异常且不能报告成功。
     *
     * @param deployment 当前部署
     * @param revision 目标修订
     * @param actionId 持久化动作 ID
     */
    void applyRevision(ModelDeployment deployment, DeploymentRevision revision, String actionId);

    /**
     * 在限定时间内等待运行时 readiness。
     *
     * @param deployment 当前部署
     * @param timeout 最大等待时间
     * @return 最终运行时检查结果
     */
    RuntimeInspection waitUntilReady(ModelDeployment deployment, Duration timeout);

    /**
     * 验证失败时恢复到之前修订，恢复本身也是远程副作用。
     *
     * @param deployment 当前部署
     * @param previousRevision 之前修订
     * @param actionId 持久化动作 ID
     */
    void restore(ModelDeployment deployment, DeploymentRevision previousRevision, String actionId);

    /** 表示运行时实际状态。 */
    record RuntimeInspection(boolean reachable, boolean ready, String status, String runtimeVersion) {
    }
}
