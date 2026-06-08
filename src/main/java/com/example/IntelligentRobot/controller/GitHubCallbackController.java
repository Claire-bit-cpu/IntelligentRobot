/*
 * GitHub Actions 回调控制器
 * 接收 GitHub Actions 工作流完成事件
 */
package com.example.IntelligentRobot.controller;

import com.example.IntelligentRobot.service.GitHubCallbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GitHub Actions 回调控制器
 * 接收工作流完成事件并发送飞书通知
 */
@RestController
@RequestMapping("/github/callback")
public class GitHubCallbackController {

    private static final Logger log = LoggerFactory.getLogger(GitHubCallbackController.class);

    @Autowired
    private GitHubCallbackService gitHubCallbackService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 处理 GitHub Actions 工作流完成回调
     *
     * @param payload    回调载荷（包含 status, environment, target, repository, ref, actor, run_id）
     * @param signature  签名（可选，用于验证请求）
     * @param deployId   部署 ID（从查询参数获取，或请求体中获取）
     * @param token      回调 token（从查询参数获取，或请求体中获取，用于验证请求合法性）
     * @return 响应实体
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "X-Callback-Signature", required = false) String signature,
            @RequestParam(value = "deployId", required = false) String deployId,
            @RequestParam(value = "token", required = false) String token
    ) {
        // 如果请求体为 null，尝试从查询参数或空 map 继续
        if (payload == null) {
            payload = new java.util.HashMap<>();
        }

        // 如果查询参数中没有 deployId/token，尝试从请求体中获取
        if ((deployId == null || token == null) && payload != null) {
            deployId = payload.containsKey("deployId") ? payload.get("deployId").toString() : deployId;
            token = payload.containsKey("token") ? payload.get("token").toString() : token;
        }

        log.info("收到 GitHub Actions 回调: status={}, environment={}, deployId={}",
                payload.get("status"), payload.get("environment"), deployId);

        try {
            // 处理回调（传入 deployId 和 token 用于验证）
            String result = gitHubCallbackService.handleCallback(payload, signature, deployId, token);

            // 立即返回 200 OK
            return ResponseEntity.ok(Map.of("code", 200, "message", result));

        } catch (Exception e) {
            log.error("处理 GitHub Actions 回调异常", e);
            return ResponseEntity.status(500)
                    .body(Map.of("code", 500, "message", "服务器内部错误"));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "GitHubCallback");
    }
}
