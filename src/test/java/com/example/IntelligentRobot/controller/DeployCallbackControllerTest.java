package com.example.IntelligentRobot.controller;

import com.example.IntelligentRobot.service.GitHubCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GitHubCallbackController 单元测试
 * 验证部署回调接口的功能
 */
class DeployCallbackControllerTest {

    @Mock
    private GitHubCallbackService gitHubCallbackService;

    @InjectMocks
    private GitHubCallbackController gitHubCallbackController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试 1：处理回调成功
     */
    @Test
    void testHandleCallback_Success() {
        // 给定：Mock 服务返回成功
        when(gitHubCallbackService.handleCallback(any(Map.class), anyString(), anyString(), anyString()))
            .thenReturn("ok");

        // 当：发送回调请求
        Map<String, Object> payload = Map.of(
            "status", "success",
            "environment", "dev",
            "repository", "Claire-bit-cpu/Test",
            "run_id", "123456",
            "deployId", "test-deploy-001",
            "actor", "test-user",
            "timestamp", "2026-06-04T22:00:00Z"
        );

        ResponseEntity<Map<String, Object>> response = 
            gitHubCallbackController.handleCallback(payload, null, null, null);

        // 那么：验证通过
        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, ((Map<String, Object>) response.getBody()).get("code"));
        assertEquals("ok", ((Map<String, Object>) response.getBody()).get("message"));
    }

    /**
     * 测试 2：处理回调失败（服务返回错误信息）
     */
    @Test
    void testHandleCallback_ServiceFailure() {
        // 给定：Mock 服务返回错误
        when(gitHubCallbackService.handleCallback(any(Map.class), anyString(), anyString(), anyString()))
            .thenReturn("token 验证失败");

        // 当：发送回调请求
        Map<String, Object> payload = Map.of(
            "status", "success",
            "environment", "dev",
            "repository", "Claire-bit-cpu/Test",
            "run_id", "123456",
            "deployId", "test-deploy-002",
            "actor", "test-user"
        );

        ResponseEntity<Map<String, Object>> response = 
            gitHubCallbackController.handleCallback(payload, null, null, null);

        // 那么：返回 200（控制器成功处理，但服务返回错误）
        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, ((Map<String, Object>) response.getBody()).get("code"));
        assertEquals("token 验证失败", ((Map<String, Object>) response.getBody()).get("message"));
    }

    /**
     * 测试 3：处理回调异常
     */
    @Test
    void testHandleCallback_Exception() {
        // 给定：Mock 服务抛出异常
        when(gitHubCallbackService.handleCallback(any(Map.class), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("服务异常"));

        // 当：发送回调请求
        Map<String, Object> payload = Map.of(
            "status", "success",
            "environment", "dev"
        );

        ResponseEntity<Map<String, Object>> response = 
            gitHubCallbackController.handleCallback(payload, null, null, null);

        // 那么：返回 500
        assertEquals(500, response.getStatusCode().value());
        assertEquals(500, ((Map<String, Object>) response.getBody()).get("code"));
        assertEquals("服务器内部错误", ((Map<String, Object>) response.getBody()).get("message"));
    }

    /**
     * 测试 4：空 Payload
     */
    @Test
    void testHandleCallback_NullPayload() {
        // 给定：Mock 服务处理空 payload
        when(gitHubCallbackService.handleCallback(any(Map.class), anyString(), anyString(), anyString()))
            .thenReturn("ok");

        // 当：发送空 payload
        ResponseEntity<Map<String, Object>> response = 
            gitHubCallbackController.handleCallback(null, null, null, null);

        // 那么：控制器创建空 map 并成功处理
        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, ((Map<String, Object>) response.getBody()).get("code"));
    }

    /**
     * 测试 5：从查询参数获取 deployId 和 token
     */
    @Test
    void testHandleCallback_WithQueryParams() {
        // 给定：Mock 服务返回成功
        when(gitHubCallbackService.handleCallback(any(Map.class), anyString(), eq("test-deploy-003"), eq("test-token")))
            .thenReturn("ok");

        // 当：发送回调请求（deployId 和 token 在查询参数中）
        Map<String, Object> payload = Map.of(
            "status", "success",
            "environment", "dev"
        );

        ResponseEntity<Map<String, Object>> response = 
            gitHubCallbackController.handleCallback(payload, null, "test-deploy-003", "test-token");

        // 那么：验证通过
        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, ((Map<String, Object>) response.getBody()).get("code"));
        
        // 验证服务被调用时使用了正确的参数
        verify(gitHubCallbackService).handleCallback(any(Map.class), anyString(), eq("test-deploy-003"), eq("test-token"));
    }

    /**
     * 测试 6：从 Payload 中获取 deployId 和 token
     */
    @Test
    void testHandleCallback_WithPayloadParams() {
        // 给定：Mock 服务返回成功
        when(gitHubCallbackService.handleCallback(any(Map.class), anyString(), eq("test-deploy-004"), eq("test-token-2")))
            .thenReturn("ok");

        // 当：发送回调请求（deployId 和 token 在 payload 中）
        Map<String, Object> payload = Map.of(
            "status", "success",
            "environment", "dev",
            "deployId", "test-deploy-004",
            "token", "test-token-2"
        );

        ResponseEntity<Map<String, Object>> response = 
            gitHubCallbackController.handleCallback(payload, null, null, null);

        // 那么：验证通过
        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, ((Map<String, Object>) response.getBody()).get("code"));
    }

    /**
     * 测试 7：健康检查接口
     */
    @Test
    void testHealthEndpoint() {
        // 当：访问健康检查接口
        Map<String, String> response = 
            gitHubCallbackController.health();

        // 那么：返回健康状态
        assertEquals("ok", response.get("status"));
        assertEquals("GitHubCallback", response.get("service"));
    }
}
