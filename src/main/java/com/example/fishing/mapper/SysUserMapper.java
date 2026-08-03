package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
