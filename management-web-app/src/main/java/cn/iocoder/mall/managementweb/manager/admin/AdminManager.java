package cn.iocoder.mall.managementweb.manager.admin;

import cn.iocoder.common.framework.util.CollectionUtils;
import cn.iocoder.common.framework.vo.CommonResult;
import cn.iocoder.common.framework.vo.PageResult;
import cn.iocoder.mall.managementweb.controller.admin.dto.AdminCreateDTO;
import cn.iocoder.mall.managementweb.controller.admin.dto.AdminPageDTO;
import cn.iocoder.mall.managementweb.controller.admin.dto.AdminUpdateInfoDTO;
import cn.iocoder.mall.managementweb.controller.admin.dto.AdminUpdateStatusDTO;
import cn.iocoder.mall.managementweb.controller.admin.vo.AdminPageItemVO;
import cn.iocoder.mall.managementweb.controller.admin.vo.AdminVO;
import cn.iocoder.mall.managementweb.convert.admin.AdminConvert;
import cn.iocoder.mall.systemservice.rpc.admin.AdminRpc;
import cn.iocoder.mall.systemservice.rpc.admin.DepartmentRpc;
import cn.iocoder.mall.systemservice.rpc.admin.vo.DepartmentVO;
import cn.iocoder.mall.systemservice.rpc.permission.PermissionRpc;
import cn.iocoder.mall.systemservice.rpc.permission.RoleRpc;
import cn.iocoder.mall.systemservice.rpc.permission.vo.RoleVO;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.*;

@Service
@Validated
public class AdminManager {

    @Reference(version = "${dubbo.consumer.AdminRpc.version}")
    private AdminRpc adminRpc;
    @Reference(version = "${dubbo.consumer.RoleRpc.version}")
    private RoleRpc roleRpc;
    @Reference(version = "${dubbo.consumer.DepartmentRpc.version}")
    private DepartmentRpc departmentRpc;
    @Reference(version = "${dubbo.consumer.PermissionRpc.version}")
    private PermissionRpc permissionRpc;

    public PageResult<AdminPageItemVO> pageAdmin(AdminPageDTO pageDTO) {
        CommonResult<PageResult<cn.iocoder.mall.systemservice.rpc.admin.vo.AdminVO>> pageResult =
                adminRpc.pageAdmin(AdminConvert.INSTANCE.convert(pageDTO));
        pageResult.checkError();
        // ????????????
        PageResult<AdminPageItemVO> adminPageVO = new PageResult<>();
        adminPageVO.setTotal(pageResult.getData().getTotal());
        adminPageVO.setList(new ArrayList<>(pageResult.getData().getList().size()));
        // ????????????
        if (!pageResult.getData().getList().isEmpty()) {
            // ??????????????????
            Map<Integer, List<RoleVO>> adminRoleMap = this.listAdminRoles(CollectionUtils.convertList(pageResult.getData().getList(),
                    cn.iocoder.mall.systemservice.rpc.admin.vo.AdminVO::getId));
            // ????????????
            CommonResult<List<DepartmentVO>> listDepartmentsResult = departmentRpc.listDepartments(
                    CollectionUtils.convertSet(pageResult.getData().getList(),
                    cn.iocoder.mall.systemservice.rpc.admin.vo.AdminVO::getDepartmentId));
            listDepartmentsResult.checkError();
            Map<Integer, DepartmentVO> departmentMap = CollectionUtils.convertMap(listDepartmentsResult.getData(), DepartmentVO::getId);
            // ????????????
            for (cn.iocoder.mall.systemservice.rpc.admin.vo.AdminVO adminVO : pageResult.getData().getList()) {
                AdminPageItemVO adminPageItemVO = AdminConvert.INSTANCE.convert02(adminVO);
                adminPageVO.getList().add(adminPageItemVO);
                // ????????????
                adminPageItemVO.setDepartment(AdminConvert.INSTANCE.convert(departmentMap.get(adminVO.getDepartmentId())));
                // ????????????
                adminPageItemVO.setRoles( AdminConvert.INSTANCE.convertList(adminRoleMap.get(adminVO.getId())));
            }
        } else {
            adminPageVO.setList(Collections.emptyList());
        }
        return adminPageVO;
    }

    private Map<Integer, List<RoleVO>> listAdminRoles(List<Integer> adminIds) {
        // ??????????????????????????????
        CommonResult<Map<Integer, Set<Integer>>> mapAdminRoleIdsResult = permissionRpc.mapAdminRoleIds(adminIds);
        mapAdminRoleIdsResult.checkError();
        // ??????????????????
        Set<Integer> roleIds = new HashSet<>();
        mapAdminRoleIdsResult.getData().values().forEach(roleIds::addAll);
        CommonResult<List<RoleVO>> listRolesResult = roleRpc.listRoles(roleIds);
        listRolesResult.checkError();
        Map<Integer, RoleVO> roleVOMap = CollectionUtils.convertMap(listRolesResult.getData(), RoleVO::getId);
        // ????????????
        Map<Integer, List<RoleVO>> adminRoleVOMap = new HashMap<>();
        mapAdminRoleIdsResult.getData().forEach((adminId, adminRoleIds) -> {
            List<RoleVO> roleVOs = new ArrayList<>(adminRoleIds.size());
            adminRoleIds.forEach(roleId -> {
                RoleVO roleVO = roleVOMap.get(roleId);
                if (roleVO != null) {
                    roleVOs.add(roleVO);
                }
            });
            adminRoleVOMap.put(adminId, roleVOs);
        });
        return adminRoleVOMap;
    }

    public Integer createAdmin(AdminCreateDTO createDTO, Integer createAdminId, String createIp) {
        CommonResult<Integer> createAdminResult = adminRpc.createAdmin(AdminConvert.INSTANCE.convert(createDTO)
            .setCreateAdminId(createAdminId).setCreateIp(createIp));
        createAdminResult.checkError();
        return createAdminResult.getData();
    }

    public void updateAdmin(AdminUpdateInfoDTO updateInfoDTO) {
        CommonResult<Boolean> updateAdminResult = adminRpc.updateAdmin(AdminConvert.INSTANCE.convert(updateInfoDTO));
        updateAdminResult.checkError();
    }

    public void updateAdminStatus(@Valid AdminUpdateStatusDTO updateStatusDTO) {
        CommonResult<Boolean> updateAdminResult = adminRpc.updateAdmin(AdminConvert.INSTANCE.convert(updateStatusDTO));
        updateAdminResult.checkError();
    }

    public AdminVO getAdmin(Integer adminId) {
        CommonResult<cn.iocoder.mall.systemservice.rpc.admin.vo.AdminVO> getAdminResult = adminRpc.getAdmin(adminId);
        getAdminResult.checkError();
        return AdminConvert.INSTANCE.convert(getAdminResult.getData());
    }

}
