package com.example.fishing.service;

import com.example.fishing.dto.MerchantStaffCreateDTO;
import com.example.fishing.dto.MerchantStaffUpdateDTO;
import com.example.fishing.entity.MerchantStaff;

import java.util.List;

/**
 * 商家员工服务
 */
public interface MerchantStaffService {

    /**
     * 查询当前商家的员工列表
     */
    List<MerchantStaff> list(Long merchantId, String keyword);

    /**
     * 新增员工并创建系统账号
     */
    void create(Long merchantId, MerchantStaffCreateDTO dto);

    /**
     * 编辑员工
     */
    void update(Long merchantId, Long id, MerchantStaffUpdateDTO dto);

    /**
     * 更新员工状态（启用/禁用）
     */
    void updateStatus(Long merchantId, Long id, String status);

    /**
     * 重置员工密码，返回明文新密码
     */
    String resetPassword(Long merchantId, Long id, String newPassword);

    /**
     * 删除员工，统一软删除
     */
    void delete(Long merchantId, Long id);
}
