package com.synapxnet.mlopslogin.service;

import com.synapxnet.mlopslogin.entity.OrganizationMembership;
import com.synapxnet.mlopslogin.entity.OrganizationTreeNode;
import com.synapxnet.mlopslogin.entity.User;
import com.synapxnet.mlopslogin.mapper.OrganizationMapper;
import com.synapxnet.mlopslogin.mapper.UserMapper;
import com.synapxnet.mlopslogin.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationAccessServiceTest {
    private JwtUtil jwtUtil;
    private OrganizationMapper organizationMapper;
    private OrganizationAccessService service;
    private UserMapper userMapper;
    private ValueOperations<String, String> valueOperations;

    /** 初始化组织权限服务所需的隔离依赖。 */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        organizationMapper = mock(OrganizationMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        userMapper = mock(UserMapper.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new OrganizationAccessService(jwtUtil, organizationMapper, redisTemplate, userMapper);
    }

    /** 验证有效用户只能获得数据库明确绑定的组织路径。 */
    @Test
    void returnsOnlyExplicitMemberships() {
        User user = new User();
        user.setRoles_failure_time(LocalDateTime.now().plusDays(1));
        OrganizationMembership membership = membership(
                "TEN-SYNAPXNET",
                "SynapXnet",
                "DEPT-SYNAPXNET-PLATFORM",
                "智能平台部",
                "TEAM-GOAI-INFRA",
                "GOAI Infrastructure 联合团队"
        );
        when(jwtUtil.extractUsername("valid-token")).thenReturn("17870171303");
        when(userMapper.findByPhone("17870171303")).thenReturn(user);
        when(organizationMapper.findActiveMembershipsByPhone("17870171303")).thenReturn(List.of(membership));

        List<OrganizationTreeNode> result = service.getVisibleOrganizationTree("Bearer valid-token");

        assertEquals(1, result.size());
        assertEquals("TEN-SYNAPXNET", result.get(0).getValue());
        assertEquals("TEAM-GOAI-INFRA", result.get(0).getChildren().get(0).getChildren().get(0).getValue());
    }

    /** 验证缺少认证头时不会返回任何组织数据。 */
    @Test
    void rejectsMissingBearerToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getVisibleOrganizationTree(null)
        );
    }

    /** 验证过期角色不能继续读取组织成员关系。 */
    @Test
    void rejectsExpiredRole() {
        User user = new User();
        user.setRoles_failure_time(LocalDateTime.now().minusMinutes(1));
        when(jwtUtil.extractUsername("valid-token")).thenReturn("17870171303");
        when(userMapper.findByPhone("17870171303")).thenReturn(user);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getVisibleOrganizationTree("Bearer valid-token")
        );
    }

    /** 创建测试使用的扁平组织成员关系。 */
    private OrganizationMembership membership(
            String tenantUid,
            String tenantName,
            String deptUid,
            String deptName,
            String teamUid,
            String teamName
    ) {
        OrganizationMembership membership = new OrganizationMembership();
        membership.setTenantUid(tenantUid);
        membership.setTenantName(tenantName);
        membership.setDeptUid(deptUid);
        membership.setDeptName(deptName);
        membership.setTeamUid(teamUid);
        membership.setTeamName(teamName);
        return membership;
    }
}
