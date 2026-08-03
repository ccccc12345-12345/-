package com.example.fishing.notify;

import com.example.fishing.controller.merchant.MerchantBaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 商家 SSE 推送接口
 */
@Tag(name = "商家实时推送")
@RestController
@RequestMapping("/api/merchant/sse")
public class MerchantSseController extends MerchantBaseController {

    @Autowired
    private MerchantSseService merchantSseService;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 SSE 实时推送连接")
    public SseEmitter connect(@RequestParam(required = false) String clientId) {
        Long merchantId = requireMerchantId();
        return merchantSseService.connect(merchantId, clientId);
    }
}
