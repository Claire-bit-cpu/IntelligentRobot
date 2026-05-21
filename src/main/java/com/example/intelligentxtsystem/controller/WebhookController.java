package com.example.intelligentxtsystem.controller;

import com.example.intelligentxtsystem.client.FeishuClient;
import com.example.intelligentxtsystem.config.FeishuSignatureVerifier;
import com.example.intelligentxtsystem.dto.FeishuCallback;
import com.example.intelligentxtsystem.dto.MessageContent;
import com.example.intelligentxtsystem.service.MessageDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书 Webhook 回调入口
 * 处理所有飞书事件
 */
@RestController
@RequestMapping("/feishu")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    // 消息去重缓存（messageId -> 处理时间）
    private final ConcurrentHashMap<String, Long> processedMessages = new ConcurrentHashMap<>();

    @Value("${dedup.window-ms}")
    private long dedupWindowMs;

    @Value("${dedup.cleanup-ms}")
    private long dedupCleanupMs;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeishuClient feishuClient;

    @Autowired
    private MessageDispatcher messageDispatcher;

    @Autowired
    private FeishuSignatureVerifier signatureVerifier;

    /**
     * 飞书回调入口
     */
    @PostMapping("/webhook")
    public Object handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lark-Request-Signature", required = false) String signature,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 添加 ngrok 跳过警告
        response.setHeader("ngrok-skip-browser-warning", "true");

        log.info("收到飞书请求: timestamp={}, hasBody={}", timestamp, rawBody != null);

        // 签名验证（如果配置了加密密钥）
        if (!signatureVerifier.verify(timestamp, signature, rawBody)) {
            log.warn("签名验证失败");
            return Map.of("code", 401, "msg", "签名验证失败");
        }

        try {
            // 解析请求体
            Map<String, Object> body = objectMapper.readValue(rawBody, Map.class);

            // URL 验证请求
            if ("url_verification".equals(body.get("type"))) {
                log.info("处理 URL 验证请求");
                return Map.of("challenge", body.get("challenge"));
            }

            // 消息事件处理
            if (body.containsKey("header")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> header = (Map<String, Object>) body.get("header");
                String eventType = (String) header.get("event_type");

                if ("im.message.receive_v1".equals(eventType)) {
                    return handleMessageEvent(body);
                }
            }

            return Map.of("code", 0, "msg", "ok");

        } catch (Exception e) {
            log.error("处理请求异常", e);
            return Map.of("code", 500, "msg", "服务器内部错误");
        }
    }

    /**
     * 处理消息事件
     */
    private Object handleMessageEvent(Map<String, Object> body) {
        try {
            // 解析回调对象
            FeishuCallback callback = objectMapper.convertValue(body, FeishuCallback.class);

            // 消息去重
            String messageId = callback.getEvent().getMessage().getMessageId();
            long now = System.currentTimeMillis();
            Long lastTime = processedMessages.putIfAbsent(messageId, now);
            
            if (lastTime != null) {
                if (now - lastTime < dedupWindowMs) {
                    log.info("消息 {} 已在 {}ms 前处理过，跳过", messageId, now - lastTime);
                    return Map.of("code", 0, "msg", "ok");
                } else {
                    // 过期了，更新时间
                    processedMessages.put(messageId, now);
                }
            }

            // 清理过期记录（超过10分钟的旧记录）
            processedMessages.entrySet().removeIf(entry -> now - entry.getValue() > dedupCleanupMs);

            // 提取消息文本
            String text = extractText(callback);
            log.info("收到消息: {}", text);

            if (text == null || text.isEmpty()) {
                return Map.of("code", 0, "msg", "ok");
            }

            // 调用消息分发器处理指令（传递发送者信息）
            String reply = messageDispatcher.dispatch(text, callback.getEvent().getSender());

            // 发送回复
            String chatId = callback.getEvent().getMessage().getChatId();
            feishuClient.sendText(chatId, reply);

            return Map.of("code", 0, "msg", "ok");

        } catch (HttpClientErrorException e) {
            log.error("调用飞书 API 失败: {}", e.getResponseBodyAsString());
            return Map.of("code", e.getStatusCode().value(), "msg", "发送消息失败");
        } catch (Exception e) {
            log.error("处理消息事件异常", e);
            return Map.of("code", 500, "msg", "处理失败");
        }
    }

    /**
     * 提取消息文本内容
     */
    private String extractText(FeishuCallback callback) {
        try {
            String contentJson = callback.getEvent()
                    .getMessage()
                    .getContent();

            log.info("原始消息内容: {}", contentJson);

            MessageContent content = objectMapper.readValue(contentJson, MessageContent.class);
            String text = content.getText();

            log.info("解析后消息: {}", text);

            // 如果消息以 @机器人 开头，去掉这部分
            if (text != null && text.contains(" ")) {
                // 处理 "@机器人 /weather 北京" 格式
                text = text.replaceFirst("^@[^\\s]+\\s*", "");
            }

            return text;

        } catch (Exception e) {
            log.warn("解析消息内容失败", e);
            return null;
        }
    }

    /**
     * GET 请求用于验证服务是否正常运行
     */
    @GetMapping("/health")
    public Object health() {
        return Map.of("status", "ok", "service", "IntelligenTxtSystem");
    }
}
