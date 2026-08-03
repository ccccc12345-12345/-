package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.DrawResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 抽号结果 Mapper
 */
@Mapper
public interface DrawResultMapper extends BaseMapper<DrawResult> {
}
