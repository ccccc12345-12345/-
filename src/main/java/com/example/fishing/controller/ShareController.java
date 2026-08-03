package com.example.fishing.controller;

import com.example.fishing.common.Result;
import com.example.fishing.service.ShareService;
import com.example.fishing.vo.ShareBoardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 分享预览接口
 * 公开接口：/api/share/board 无需登录
 */
@Slf4j
@Tag(name = "分享预览")
@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private ShareService shareService;

    @PostMapping("/create")
    @Operation(summary = "生成分享链接")
    public Result<String> create(
            @RequestBody Map<String, Long> params,
            HttpServletRequest request) {
        Long pondId = params.get("pondId");
        Long slotId = params.get("slotId");
        String baseUrl = request.getHeader("X-Frontend-Base-Url");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = extractBaseUrl(request);
            log.warn("未携带 X-Frontend-Base-Url 头，使用请求来源作为兜底: {}", baseUrl);
        }
        log.info("生成分享链接 pondId={}, slotId={}, baseUrl={}", pondId, slotId, baseUrl);
        String url = shareService.createShareUrl(pondId, slotId, baseUrl);
        log.info("分享链接已生成: {}", url);
        return Result.success(url);
    }

    @GetMapping("/board")
    @Operation(summary = "公开钓位看板")
    public Result<ShareBoardVO> board(
            @Parameter(description = "鱼塘ID") @RequestParam Long pondId,
            @Parameter(description = "时段ID") @RequestParam Long slotId,
            @Parameter(description = "分享令牌") @RequestParam String token) {
        log.info("访问公开钓位看板 pondId={}, slotId={}", pondId, slotId);
        shareService.validateToken(pondId, slotId, token);
        return Result.success(shareService.queryPublicBoard(pondId, slotId));
    }

    /**
     * 从请求中提取前端基础地址（兜底）
     */
    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }
}
