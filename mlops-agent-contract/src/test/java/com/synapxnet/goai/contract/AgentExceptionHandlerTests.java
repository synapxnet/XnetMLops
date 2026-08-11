package com.synapxnet.goai.contract;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证公共 Agent 异常处理器优先级和错误上下文完整性。
 */
class AgentExceptionHandlerTests {

    /** 确认契约异常处理器始终使用 Spring 最高优先级。 */
    @Test
    void usesHighestAdvicePrecedence() {
        Order order = AgentExceptionHandler.class.getAnnotation(Order.class);

        assertNotNull(order);
        assertEquals(Ordered.HIGHEST_PRECEDENCE, order.value());
    }

    /** 校验控制器校验失败后，错误包络仍保留请求体中的全局 requestId。 */
    @Test
    void preservesBodyRequestIdForStructuredControllerErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AgentContract.CONTEXT_ATTRIBUTE, new AgentContract.RequestContext(
                "ws_goai_demo", "inc_test", "trace_test", "mlops.deployment.rollback",
                "idem_test", "enterprise-goai:operator", null));
        AgentContract.ToolRequest<Map<String, Object>> body = new AgentContract.ToolRequest<>(
                "req_test", "mlops.deployment.rollback", Map.of(), null, null, null, null,
                null, null, null, null, null, null, null, false);

        AgentContract.requireContext(request, "mlops.deployment.rollback", body);
        var response = new AgentExceptionHandler().handle(
                new AgentContractException(403, "APPROVAL_REQUIRED", "审批缺失"), request);

        assertEquals("req_test", response.getBody().meta().requestId());
    }
}

