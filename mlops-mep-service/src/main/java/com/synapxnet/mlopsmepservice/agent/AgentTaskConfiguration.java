/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * MLOps 决赛功能实现 / MLOps finals component implementation.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.mlopsmepservice.agent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 为持久化部署动作提供有界队列、拒绝策略和优雅停机执行器。
 */
@Configuration
public class AgentTaskConfiguration {

    /**
     * 创建受控回滚执行器，应用关闭时等待在途动作更新状态。
     *
     * @return 有界 TaskExecutor
     */
    @Bean("deploymentActionExecutor")
    public TaskExecutor deploymentActionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("goai-deployment-action-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(32);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 创建受控 readiness 调度器，取消轮询时立即移除任务。
     *
     * @return Spring 管理的有界调度器
     */
    @Bean("deploymentReadinessScheduler")
    public ThreadPoolTaskScheduler deploymentReadinessScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("goai-deployment-readiness-");
        scheduler.setPoolSize(2);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }
}
