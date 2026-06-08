/*
 * GitHub Actions 回调服务
 * 处理 GitHub Actions 工作流完成事件，通过 NotificationService 发送飞书通知
 */
package com.example.IntelligentRobot.service;

import com.example.IntelligentRobot.config.GitHubConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * GitHub Actions 回调服务
 * 处理工作流完成事件，通过 NotificationService 发送飞书通知
 * 
 * 复用项目统一的通知链路（NotificationService），无需额外配置飞书Webhook
 * 通知会发送到 application.yaml 中配置的 notification.default-chat-ids 群聊
 */
@Service
public class GitHubCallbackService {

    private static final Logger log = LoggerFactory.getLogger(GitHubCallbackService.class);

    @Autowired
    private GitHubConfig gitHubConfig;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Redis 模板，用于验证回调 token
     */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 统一通知服务（非强制依赖，未配置时不会报错）
     * 复用项目现有的通知链路，支持消息去重、合并、多群聊配置
     */
    @Autowired(required = false)
    private NotificationService notificationService;

    /**
     * 监控群聊 ID（用于任务监控面板）
     */
    @org.springframework.beans.factory.annotation.Value("${task.monitor.chat-id:}")
    private String monitorChatId;

    /**
     * 默认通知群聊 ID 列表（逗号分隔）
     */
    @org.springframework.beans.factory.annotation.Value("${notification.default-chat-ids:}")
    private String defaultChatIds;

    /**
     * 处理 GitHub Actions 回调
     *
     * @param payload    回调载荷
     * @param signature  签名（用于验证请求）
     * @param deployId   部署 ID（用于验证 token，可从查询参数或 payload 中获取）
     * @param token      回调 token（用于验证请求合法性，可从查询参数或 payload 中获取）
     * @return 处理结果
     */
    public String handleCallback(Map<String, Object> payload, String signature, String deployId, String token) {
        // 尝试从 payload 中获取 deployId 和 token（兼容不同的回调方式）
        if (deployId == null && payload.containsKey("deployId")) {
            deployId = payload.get("deployId").toString();
        }
        if (token == null && payload.containsKey("token")) {
            token = payload.get("token").toString();
        }

        log.info("收到 GitHub Actions 回调: deployId={}, status={}, environment={}",
                deployId, payload.get("status"), payload.get("environment"));

        try {
            // 1. 验证 token（如果提供了 deployId 和 token）
            if (!verifyToken(deployId, token)) {
                return "token 验证失败";
            }

            // 2. 验证签名（如果配置了 secret）
            if (!verifySignature(payload, signature)) {
                return "签名验证失败";
            }

            // 3. 解析回调数据
            String status = (String) payload.getOrDefault("status", "unknown");
            String environment = (String) payload.getOrDefault("environment", "unknown");
            String target = (String) payload.getOrDefault("target", "unknown");
            String repository = (String) payload.getOrDefault("repository", "unknown");
            String ref = (String) payload.getOrDefault("ref", "unknown");
            String actor = (String) payload.getOrDefault("actor", "unknown");
            String runId = (String) payload.getOrDefault("run_id", payload.getOrDefault("runId", "unknown").toString());

            // 4. 处理回调数据（部署完成通知已取消）
            sendNotification(status, environment, target, repository, ref, actor, runId, deployId);

            return "ok";

        } catch (Exception e) {
            log.error("处理 GitHub Actions 回调异常", e);
            return "处理失败: " + e.getMessage();
        }
    }

    /**
     * 发送通知（优先更新卡片，失败则降级为文本通知）
     * 复用项目统一的通知链路，无需额外配置飞书Webhook
     *
     * @param status 部署状态（success/failure）
     * @param environment 部署环境（dev/test/staging/prod）
     * @param target 部署目标
     * @param repository 仓库名称
     * @param ref 分支/Tag/SHA
     * @param actor 操作者
     * @param runId GitHub Actions 运行 ID
     * @param deployId 部署 ID（用于查找关联的 taskId 和 messageId）
     */
    private void sendNotification(
            String status,
            String environment,
            String target,
            String repository,
            String ref,
            String actor,
            String runId,
            String deployId
    ) {
        if (notificationService == null) {
            log.warn("NotificationService 未注入，无法发送通知");
            return;
        }

        try {
            // 1. 先发送部署成功/失败的通知消息
            String message = buildMessage(status, environment, target, repository, ref, actor, runId);
            String defaultChatId = getDefaultChatId();
            boolean notificationSent;
            if (defaultChatId != null) {
                // sendUrgentNotification(chatId, eventType, content)
                notificationSent = notificationService.sendUrgentNotification(defaultChatId, "DEPLOY", message);
            } else {
                // sendNotification(content) - 使用默认 chatId
                notificationSent = notificationService.sendNotification(message);
            }
            if (notificationSent) {
                log.info("已发送部署通知: status={}, environment={}", status, environment);
            } else {
                log.warn("发送部署通知失败: status={}, environment={}", status, environment);
            }

        } catch (Exception e) {
            log.error("发送部署通知异常", e);
        }
    }

    /**
     * 构建通知消息
     */
    private String buildMessage(
            String status,
            String environment,
            String target,
            String repository,
            String ref,
            String actor,
            String runId
    ) {
        String timestamp = OffsetDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String statusEmoji = "success".equals(status) ? "✅" : "❌";
        String statusText = "success".equals(status) ? "部署成功" : "部署失败";

        String detailsUrl = "https://github.com/" + repository + "/actions/runs/" + runId;

        return String.format(
                "%s Java 部署%s%n%n" +
                "**环境**: %s (Java)%n" +
                "**部署目标**: %s%n" +
                "**仓库**: %s%n" +
                "**分支**: %s%n" +
                "**操作者**: %s%n" +
                "**时间**: %s%n" +
                "**状态**: %s%n" +
                "查看详情: %s",
                statusEmoji, statusText,
                environment, target, repository, ref, actor, timestamp, status,
                detailsUrl
        );
    }

    /**
     * 获取默认群聊 ID（从配置中读取）
     */
    private String getDefaultChatId() {
        // 优先使用 task.monitor.chat-id（监控群）
        if (monitorChatId != null && !monitorChatId.isEmpty()) {
            return monitorChatId;
        }
        
        // 其次使用 notification.default-chat-ids（默认通知群）
        if (defaultChatIds != null && !defaultChatIds.isEmpty()) {
            // 取第一个群聊 ID
            String[] chatIds = defaultChatIds.split(",");
            return chatIds[0].trim();
        }
        
        return null;
    }

    /**
     * 验证回调 token
     *
     * @param deployId 部署 ID
     * @param token    回调 token
     * @return 是否有效
     */
    private boolean verifyToken(String deployId, String token) {
        // 如果未提供 deployId 或 token，跳过校验（兼容旧版本）
        if (deployId == null || deployId.isEmpty() || token == null || token.isEmpty()) {
            log.warn("未提供 deployId 或 token，跳过 token 校验");
            return true;
        }

        // 如果 Redis 不可用，跳过校验
        if (stringRedisTemplate == null) {
            log.warn("StringRedisTemplate 不可用，跳过 token 校验");
            return true;
        }

        try {
            // 从 Redis 读取存储的 token
            String redisKey = "deploy:callback:token:" + deployId;
            String storedToken = stringRedisTemplate.opsForValue().get(redisKey);

            if (storedToken == null) {
                log.warn("Redis 中未找到 deployId 对应的 token: {}", deployId);
                return false;
            }

            // 验证 token 是否匹配
            boolean isValid = storedToken.equals(token);
            if (isValid) {
                // 验证成功后，删除 Redis 中的 token（一次性使用）
                stringRedisTemplate.delete(redisKey);
                log.info("回调 token 验证成功，已删除 Redis 中的 token: deployId={}", deployId);
            } else {
                log.warn("回调 token 不匹配: deployId={}", deployId);
            }

            return isValid;

        } catch (Exception e) {
            log.error("验证回调 token 异常", e);
            return false;
        }
    }

    /**
     * 验证签名
     *
     * @param payload 回调载荷
     * @param signature 签名
     * @return 是否有效
     */
    private boolean verifySignature(Map<String, Object> payload, String signature) {
        String secret = gitHubConfig.getCallback().getSecret();

        // 如果未配置 secret，跳过校验
        if (secret == null || secret.isEmpty()) {
            log.warn("GitHub callback secret 未配置，跳过签名校验");
            return true;
        }

        if (signature == null || signature.isEmpty()) {
            log.warn("缺少签名");
            return false;
        }

        try {
            // 将 payload 转换为 JSON 字符串
            String payloadJson = objectMapper.writeValueAsString(payload);

            // 计算签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));

            // 将字节数组转换为十六进制字符串
            StringBuilder actualSignature = new StringBuilder();
            for (byte b : hash) {
                actualSignature.append(String.format("%02x", b));
            }

            boolean isValid = signature.equals(actualSignature.toString());
            if (!isValid) {
                log.warn("签名不匹配");
            }

            return isValid;

        } catch (Exception e) {
            log.error("验证签名异常", e);
            return false;
        }
    }
}
