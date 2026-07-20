package com.synapxnet.mlopssmpservice.mapper;

import com.synapxnet.mlopssmpservice.entity.Department;
import com.synapxnet.mlopssmpservice.entity.Team;
import com.synapxnet.mlopssmpservice.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptTreeDataMapper {
    @Select("SELECT uid, tenant_id AS tenantId, tenant_name AS tenantName, status FROM xnet_mlops_sys_tenant WHERE status = 1")
    List<Tenant> findAllEnabledTenants();

    @Select("SELECT uid, tenant_uid AS tenantUid, dept_id AS deptId, dept_name AS deptName, status FROM xnet_mlops_sys_department WHERE status = 1 AND tenant_uid = #{tenantUid}")
    List<Department> findEnabledDepartmentsByTenantUid(String tenantUid);

    @Select("SELECT uid, dept_uid AS deptUid, team_id AS teamId, team_name AS teamName, status FROM xnet_mlops_sys_team WHERE status = 1 AND dept_uid = #{deptUid}")
    List<Team> findEnabledTeamsByDeptUid(String deptUid);
}
