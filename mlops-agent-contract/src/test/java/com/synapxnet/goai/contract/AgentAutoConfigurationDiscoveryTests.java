/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 治理鉴权自动发现回归 / Governed authentication discovery regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
 * Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
 */
package com.synapxnet.goai.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAutoConfigurationDiscoveryTests {
    /** 独立构建的契约包必须可自动发现鉴权过滤器。The standalone contract artifact must expose its authentication filter automatically. */
    @Test
    void discoversConfigurationAndRegistersGovernanceBeans() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(AgentContractAutoConfiguration.class.getName());
        new ApplicationContextRunner()
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withConfiguration(AutoConfigurations.of(AgentContractAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DelegatedTokenVerifier.class);
                    assertThat(context).hasSingleBean(GovernedApprovalVerifier.class);
                    assertThat(context).hasSingleBean(AgentExceptionHandler.class);
                    assertThat(context.getBean("agentRequestContextFilter", FilterRegistrationBean.class)
                            .getUrlPatterns()).containsExactly("/api/agent/v1/*");
                });
    }
}
