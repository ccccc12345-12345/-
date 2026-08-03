package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.CatchRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 渔获记录 Mapper
 */
@Mapper
public interface CatchRecordMapper extends BaseMapper<CatchRecord> {
}
