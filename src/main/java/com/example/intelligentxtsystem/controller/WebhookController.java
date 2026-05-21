package com.example.intelligentxtsystem.controller;

import com.example.intelligentxtsystem.config.FeishuSignatureVerifier;
import com.example.intelligentxtsystem.service.MessageProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书 Webhook 回调入口
 * 处理所有飞书事件
 */
@RestController
@RequestMapping("/feishu")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeishuSignatureVerifier signatureVerifier;

    @Autowired
    private MessageProcessor messageProcessor;

    /**
     * 飞书回调入口
     */
    @PostMapping("/webhook")
    public Object handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lark-Request-Signature", required = false) String signature,
            HttpServletResponse response
    ) {
        response.setHeader("ngrok-skip-browser-warning", "true");

        log.info("收到飞书请求: timestamp={}, hasBody={}", timestamp, rawBody != null);

        // 签名验证
        if (!signatureVerifier.verify(timestamp, signature, rawBody)) {
            log.warn("签名验证失败");
            return Map.of("code", 401, "msg", "签名验证失败");
        }

        try {
            Map<String, Object> body = objectMapper.readValue(rawBody, Map.class);

            // URL 验证请求
            if ("url_verification".equals(body.get("type"))) {
                log.info("处理 URL 验证请求");
                return Map.of("challenge", body.get("challenge"));
            }

            // 消息事件 - 委托给 MessageProcessor 异步处理
            if (body.containsKey("header")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> header = (Map<String, Object>) body.get("header");
                String eventType = (String) header.get("event_type");

                if ("im.message.receive_v1".equals(eventType)) {
                    messageProcessor.processMessageEvent(body);
                }
            }

            // 立即返回200，不等待处理完成
            return Map.of("code", 0, "msg", "ok");

        } catch (Exception e) {
            log.error("处理请求异常", e);
            return Map.of("code", 500, "msg", "服务器内部错误");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Object health() {
        return Map.of("status", "ok", "service", "IntelligenTxtSystem");
    }
}
