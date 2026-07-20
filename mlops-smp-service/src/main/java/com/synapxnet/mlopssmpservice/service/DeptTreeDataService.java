package com.synapxnet.mlopssmpservice.service;

import com.synapxnet.mlopssmpservice.entity.Department;
import com.synapxnet.mlopssmpservice.entity.DeptTreeData;
import com.synapxnet.mlopssmpservice.entity.Team;
import com.synapxnet.mlopssmpservice.entity.Tenant;
import com.synapxnet.mlopssmpservice.mapper.DeptTreeDataMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeptTreeDataService {
    private final DeptTreeDataMapper deptTreeDataMapper;

    public DeptTreeDataService(DeptTreeDataMapper deptTreeDataMapper) {
        this.deptTreeDataMapper = deptTreeDataMapper;
    }

    public List<DeptTreeData> getDeptTreeData() {
        List<Tenant> tenants = deptTreeDataMapper.findAllEnabledTenants();
        return tenants.stream()
                .map(this::convertTenantToTree)
                .collect(Collectors.toList());
    }

    private DeptTreeData convertTenantToTree(Tenant tenant) {
        List<Department> departments = deptTreeDataMapper.findEnabledDepartmentsByTenantUid(tenant.getUid());

        List<DeptTreeData> departmentNodes = departments.stream()
                .map(this::convertDepartmentToTree)
                .collect(Collectors.toList());

        return new DeptTreeData(
                tenant.getTenantName(),
                tenant.getUid(),
                departmentNodes
        );
    }

    private DeptTreeData convertDepartmentToTree(Department department) {
        List<Team> teams = deptTreeDataMapper.findEnabledTeamsByDeptUid(department.getUid());

        List<DeptTreeData> teamNodes = teams.stream()
                .map(team -> new DeptTreeData(
                        team.getTeamName(),
                        team.getUid(),
                        Collections.emptyList()
                ))
                .collect(Collectors.toList());

        return new DeptTreeData(
                department.getDeptName(),
                department.getUid(),
                teamNodes
        );
    }
}