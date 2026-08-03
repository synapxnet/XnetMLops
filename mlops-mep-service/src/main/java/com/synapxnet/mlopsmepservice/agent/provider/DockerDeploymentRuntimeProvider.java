package com.synapxnet.mlopsmepservice.agent.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.mlopsmepservice.agent.DeploymentRevision;
import com.synapxnet.mlopsmepservice.entity.ModelDeployment;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 使用本机受控 Docker API 执行真实部署修订切换，不通过任意 SSH 命令模板。
 */
@Component
public class DockerDeploymentRuntimeProvider implements DeploymentRuntimeProvider {

    private final ObjectMapper objectMapper;
    private final TaskScheduler readinessScheduler;
    private final String runtimeNetwork;

    /**
     * 创建 Docker Runtime Provider。
     *
     * @param objectMapper 部署规格 JSON 解析器
     * @param readinessScheduler Spring 管理的 readiness 轮询调度器
     * @param runtimeNetwork 仅供 MEP、Docker Proxy 和模型容器通信的隔离网络
     */
    public DockerDeploymentRuntimeProvider(
            ObjectMapper objectMapper,
            @Qualifier("deploymentReadinessScheduler") TaskScheduler readinessScheduler,
            @Value("${openxnet.docker.runtime-network:synapxnet_runtime-control}") String runtimeNetwork) {
        this.objectMapper = objectMapper;
        this.readinessScheduler = readinessScheduler;
        if (runtimeNetwork == null || !runtimeNetwork.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")) {
            throw new IllegalArgumentException("Docker Runtime 隔离网络名称无效");
        }
        this.runtimeNetwork = runtimeNetwork;
    }

    /** 仅支持明确标记为本机 Docker 的部署节点。 */
    @Override
    public boolean supports(ModelDeployment deployment) {
        return deployment != null && ("127.0.0.1".equals(deployment.getNodeName())
                || "node_goai_local".equals(deployment.getNodeUid()));
    }

    /** 检查真实 Docker 容器状态和镜像版本。 */
    @Override
    public RuntimeInspection inspect(ModelDeployment deployment) {
        try (DockerClient client = client()) {
            InspectContainerResponse value = inspectContainer(client, containerReference(deployment));
            boolean running = value.getState() != null && Boolean.TRUE.equals(value.getState().getRunning());
            String health = value.getState() == null || value.getState().getHealth() == null
                    ? null : value.getState().getHealth().getStatus();
            boolean ready = running && (health == null || "healthy".equalsIgnoreCase(health));
            String image = value.getConfig() == null ? null : value.getConfig().getImage();
            return new RuntimeInspection(true, ready, health == null ? (running ? "running" : "stopped") : health, image);
        } catch (NotFoundException exception) {
            return new RuntimeInspection(true, false, "not_found", null);
        } catch (RuntimeException exception) {
            return new RuntimeInspection(false, false, "unavailable", null);
        } catch (Exception exception) {
            return new RuntimeInspection(false, false, "close_failed", null);
        }
    }

    /** 验证目标镜像存在于受控本机 Docker，并校验规格端口和容器名。 */
    @Override
    public void validateRevision(ModelDeployment deployment, DeploymentRevision revision) {
        RuntimeSpec spec = parseSpec(revision);
        if (!"docker".equals(spec.runtime()) || spec.containerName() == null || spec.containerName().isBlank()
                || spec.containerPort() == null || spec.hostPort() == null
                || spec.containerPort() < 1 || spec.hostPort() < 1) {
            throw new AgentContractException(412, "PRECONDITION_FAILED", "目标修订缺少有效 Docker 规格");
        }
        try (DockerClient client = client()) {
            client.inspectImageCmd(revision.getImageName()).exec();
        } catch (NotFoundException exception) {
            throw new AgentContractException(412, "PRECONDITION_FAILED", "目标修订镜像尚未进入受控节点");
        } catch (RuntimeException exception) {
            throw new AgentContractException(
                    503, "UPSTREAM_UNAVAILABLE", "Docker Runtime 不可用", true, java.util.Map.of());
        } catch (Exception exception) {
            throw new AgentContractException(
                    503, "UPSTREAM_UNAVAILABLE", "Docker Runtime 连接关闭失败", true, java.util.Map.of());
        }
    }

    /** 停止旧容器并以目标镜像创建同名容器，所有参数来自经过验证的修订规格。 */
    @Override
    public void applyRevision(ModelDeployment deployment, DeploymentRevision revision, String actionId) {
        RuntimeSpec spec = parseSpec(revision);
        try (DockerClient client = client()) {
            removeExisting(client, spec.containerName());
            ExposedPort exposedPort = ExposedPort.tcp(spec.containerPort());
            PortBinding portBinding = new PortBinding(
                    Ports.Binding.bindIpAndPort("127.0.0.1", spec.hostPort()), exposedPort);
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPortBindings(portBinding)
                    .withNetworkMode(runtimeNetwork);
            String containerId = client.createContainerCmd(revision.getImageName())
                    .withName(spec.containerName())
                    .withExposedPorts(exposedPort)
                    .withHostConfig(hostConfig)
                    .withLabels(java.util.Map.of(
                            "com.synapxnet.goai.action", actionId,
                            "com.synapxnet.goai.revision", String.valueOf(revision.getRevisionNumber())))
                    .exec().getId();
            client.startContainerCmd(containerId).exec();
            deployment.setContainerId(containerId);
            deployment.setContainerName(spec.containerName());
        } catch (RuntimeException exception) {
            throw new AgentContractException(
                    503, "UPSTREAM_UNAVAILABLE", "Docker 修订切换失败", true, java.util.Map.of());
        } catch (Exception exception) {
            throw new AgentContractException(
                    503, "UPSTREAM_UNAVAILABLE", "Docker 修订切换连接关闭失败", true, java.util.Map.of());
        }
    }

    /** 在限定时间内轮询 Docker 状态；超时返回未就绪而不伪造成功。 */
    @Override
    public RuntimeInspection waitUntilReady(ModelDeployment deployment, Duration timeout) {
        AtomicReference<RuntimeInspection> last = new AtomicReference<>(inspect(deployment));
        if (last.get().ready()) {
            return last.get();
        }
        CompletableFuture<RuntimeInspection> ready = new CompletableFuture<>();
        ScheduledFuture<?> polling = readinessScheduler.scheduleAtFixedRate(() -> {
            RuntimeInspection current = inspect(deployment);
            last.set(current);
            if (current.ready()) {
                ready.complete(current);
            }
        }, Duration.ofMillis(500L));
        try {
            return ready.get(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            return last.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            RuntimeInspection current = last.get();
            return new RuntimeInspection(current.reachable(), false, "interrupted", current.runtimeVersion());
        } catch (ExecutionException exception) {
            RuntimeInspection current = last.get();
            return new RuntimeInspection(current.reachable(), false, "poll_failed", current.runtimeVersion());
        } finally {
            polling.cancel(false);
        }
    }

    /** 使用同一受控执行路径恢复先前修订。 */
    @Override
    public void restore(ModelDeployment deployment, DeploymentRevision previousRevision, String actionId) {
        validateRevision(deployment, previousRevision);
        applyRevision(deployment, previousRevision, actionId + "-restore");
    }

    /** 创建使用环境 DOCKER_HOST/TLS 配置的 Docker Client。 */
    private DockerClient client() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(20)
                .connectionTimeout(Duration.ofSeconds(3))
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    /** 根据持久化容器 ID 或名称检查容器。 */
    private InspectContainerResponse inspectContainer(DockerClient client, String reference) {
        return client.inspectContainerCmd(reference).exec();
    }

    /** 获取部署当前容器引用。 */
    private String containerReference(ModelDeployment deployment) {
        return deployment.getContainerId() != null && !deployment.getContainerId().isBlank()
                ? deployment.getContainerId() : deployment.getContainerName();
    }

    /** 停止并删除同名旧容器；容器不存在属于正常首次部署。 */
    private void removeExisting(DockerClient client, String containerName) {
        try {
            InspectContainerResponse value = inspectContainer(client, containerName);
            if (value.getState() != null && Boolean.TRUE.equals(value.getState().getRunning())) {
                client.stopContainerCmd(containerName).withTimeout(20).exec();
            }
            client.removeContainerCmd(containerName).withForce(true).exec();
        } catch (NotFoundException ignored) {
            // 首次部署没有旧容器，不属于失败。
        }
    }

    /** 解析并校验修订中的非敏感 Runtime 规格。 */
    private RuntimeSpec parseSpec(DeploymentRevision revision) {
        try {
            return objectMapper.readValue(revision.getSpecJson(), RuntimeSpec.class);
        } catch (JsonProcessingException exception) {
            throw new AgentContractException(412, "PRECONDITION_FAILED", "目标修订规格无法解析");
        }
    }

    /** 表示 Docker 修订中的白名单部署规格。 */
    private record RuntimeSpec(
            String runtime,
            String containerName,
            String image,
            Integer containerPort,
            Integer hostPort,
            Integer replicas,
            java.util.List<String> secretRefs) {
    }
}
