package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.FishingSpot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FishingSpotMapper extends BaseMapper<FishingSpot> {

    /**
     * Pick an enabled spot from the same pond and exclude spots already assigned
     * in the same time slot. This stays compatible with older MySQL/MariaDB.
     */
    @Select("SELECT s.* FROM fishing_spot s " +
            "WHERE s.status = 1 " +
            "AND s.pond_id = (SELECT t.pond_id FROM time_slot t WHERE t.id = #{slotId}) " +
            "AND s.id NOT IN (SELECT d.spot_id FROM draw_result d WHERE d.slot_id = #{slotId} AND d.spot_id IS NOT NULL) " +
            "ORDER BY RAND() LIMIT 1")
    FishingSpot selectRandomAvailableSpot(@Param("slotId") Long slotId);
}
