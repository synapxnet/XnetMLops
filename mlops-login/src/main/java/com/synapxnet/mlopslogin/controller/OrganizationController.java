package com.synapxnet.mlopslogin.controller;

import com.synapxnet.mlopslogin.entity.OrganizationTreeNode;
import com.synapxnet.mlopslogin.service.OrganizationAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class OrganizationController {
    private final OrganizationAccessService organizationAccessService;

    /**
     * 创建组织查询控制器。
     *
     * @param organizationAccessService 组织权限服务
     */
    public OrganizationController(OrganizationAccessService organizationAccessService) {
        this.organizationAccessService = organizationAccessService;
    }

    /**
     * 返回当前登录用户可见的租户、部门和团队树。
     *
     * @param authorization Bearer 认证头
     * @return 当前用户可见的组织树响应
     */
    @GetMapping("/organization-tree")
    public ResponseEntity<Map<String, Object>> getOrganizationTree(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        try {
            List<OrganizationTreeNode> tree = organizationAccessService.getVisibleOrganizationTree(authorization);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", tree,
                    "error", "null"
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "code", 401,
                    "message", "Unauthorized",
                    "data", List.of(),
                    "error", "Invalid or expired token"
            ));
        }
    }

    /**
     * 为 Nginx 子请求校验当前用户是否可以读取指定团队数据。
     *
     * @param authorization Bearer 认证头
     * @param tenantUid 租户唯一编码
     * @param deptUid 部门唯一编码
     * @param teamUid 团队唯一编码
     * @return 允许时返回 204，无数据权时返回 403
     */
    @GetMapping("/organization-access")
    public ResponseEntity<Void> checkOrganizationAccess(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Tenant-Uid", required = false) String tenantUid,
            @RequestHeader(value = "X-Dept-Uid", required = false) String deptUid,
            @RequestHeader(value = "X-Team-Uid", required = false) String teamUid
    ) {
        try {
            boolean allowed = organizationAccessService.hasDataAccess(
                    authorization,
                    tenantUid,
                    deptUid,
                    teamUid
            );
            return allowed
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
