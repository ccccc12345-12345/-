package com.example.fishing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.entity.SysUser;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户注册
     *
     * @param phone    手机号
     * @param nickname 昵称
     * @param password 明文密码
     * @return 用户ID
     */
    Long register(String phone, String nickname, String password);

    /**
     * 根据手机号查询用户
     */
    SysUser findByPhone(String phone);

    /**
     * 修改用户管理员类型及绑定鱼塘
     */
    void updateAdminBinding(Long userId, Integer adminType, Long pondId);

    /**
     * 商家注册（需邀请码）
     *
     * @param phone      手机号
     * @param nickname   昵称
     * @param password   明文密码
     * @param inviteCode 邀请码
     * @return 用户ID
     */
    Long registerMerchant(String phone, String nickname, String password, String inviteCode);
}
