package com.example.fishing.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.fishing.common.Result;
import com.example.fishing.entity.Pond;
import com.example.fishing.mapper.PondMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端鱼塘查询接口
 */
@Tag(name = "鱼塘查询")
@RestController
@RequestMapping("/api/ponds")
public class PondPublicController {

    @Autowired
    private PondMapper pondMapper;

    @GetMapping
    @Operation(summary = "获取启用中的鱼塘列表")
    public Result<List<Pond>> list() {
        List<Pond> list = pondMapper.selectList(
                new QueryWrapper<Pond>().eq("status", 1).orderByAsc("id"));
        return Result.success(list);
    }
}
