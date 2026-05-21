/*
核心发送类,发送到飞书群中
 */

package com.example.intelligentxtsystem.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class FeishuClient {

    private static final Logger log = LoggerFactory.getLogger(FeishuClient.class);

    @Value("${feishu.app-id}")
    private String appId;

    @Value("${feishu.app-secret}")
    private String appSecret;

    @Value("${feishu.api-base-url}")
    private String apiBaseUrl;

    @Value("${feishu.token-buffer-seconds}")
    private int tokenBufferSeconds;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tenantAccessToken;
    private long expireTime = 0;

    /**
     * 获取 Tenant Access Token
     */
    private void ensureToken() {
        // 如果 Token 还没过期，直接返回
        if (tenantAccessToken != null && System.currentTimeMillis() < expireTime) {
            return;
        }

        String url = apiBaseUrl + "/auth/v3/tenant_access_token/internal";
        Map<String, String> body = Map.of(
                "app_id", appId,
                "app_secret", appSecret
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
        Map<String, Object> result = response.getBody();

        tenantAccessToken = (String) result.get("tenant_access_token");
        int expire = (int) result.get("expire");
        expireTime = System.currentTimeMillis() + expire * 1000L - tokenBufferSeconds * 1000L;
    }

    /**
     * 发送文本消息
     * @param receiveId 群ID (oc_xxxx) 或 用户OpenID (ou_xxxx)
     */
    public void sendText(String receiveId, String text) {
        ensureToken();

        String url = apiBaseUrl + "/im/v1/messages?receive_id_type=chat_id";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tenantAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 飞书 API 要求 content 是 JSON 字符串，不是对象
        String content = "{\"text\":\"" + escapeJson(text) + "\"}";

        Map<String, Object> body = Map.of(
                "receive_id", receiveId,
                "msg_type", "text",
                "content", content
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    private String calendarId;

    /**
     * 创建日历日程（创建到机器人自己的日历中，可选邀请用户参与）
     *
     * @param summary     日程标题
     * @param startTime   开始时间
     * @param endTime     结束时间（默认1小时后）
     * @param attendeeId  参与者 open_id（可选，为空则不邀请）
     * @return 创建结果
     */
    public String createCalendarEventWithAttendee(String summary, LocalDateTime startTime, LocalDateTime endTime, String attendeeId) {
        ensureToken();

        // 获取机器人日历ID（如果还没有）
        if (calendarId == null) {
            calendarId = getPrimaryCalendarId();
            log.info("获取到的日历ID: {}", calendarId);
            if (calendarId == null) {
                return "获取日历失败";
            }
        }

        // 转换时间戳（飞书需要秒级时间戳）
        long startTimestamp = startTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        long endTimestamp = endTime != null
                ? endTime.atZone(ZoneId.systemDefault()).toEpochSecond()
                : startTime.plusHours(1).atZone(ZoneId.systemDefault()).toEpochSecond();

        log.info("创建日程 - 标题: {}, 开始时间戳: {}, 结束时间戳: {}, 参与者: {}", summary, startTimestamp, endTimestamp, attendeeId);

        // 先创建日程（不带参与者）
        String eventId = createCalendarEventOnly(summary, startTimestamp, endTimestamp);
        
        if (eventId == null) {
            return "创建日程失败";
        }

        // 如果有参与者，单独添加
        if (attendeeId != null && !attendeeId.isEmpty()) {
            String addResult = addAttendeeToEvent(eventId, attendeeId);
            if (!"success".equals(addResult)) {
                log.warn("添加参与者失败，但日程已创建: {}", addResult);
            }
        }

        return "success";
    }

    /**
     * 仅创建日程，不添加参与者
     */
    private String createCalendarEventOnly(String summary, long startTimestamp, long endTimestamp) {
        String url = apiBaseUrl + "/calendar/v4/calendars/" + calendarId + "/events";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tenantAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "summary", summary,
                "start_time", Map.of(
                        "timestamp", String.valueOf(startTimestamp),
                        "timezone", "Asia/Shanghai"
                ),
                "end_time", Map.of(
                        "timestamp", String.valueOf(endTimestamp),
                        "timezone", "Asia/Shanghai"
                ),
                "location", Map.of(
                        "name", "飞书群"
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = response.getBody();
            log.info("创建日程API响应: {}", responseBody);

            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    if (data != null) {
                        Map<String, Object> event = (Map<String, Object>) data.get("event");
                        if (event != null) {
                            return (String) event.get("event_id");
                        }
                    }
                }
                return null;
            }
            log.error("创建日程失败: {}", responseBody);
            return null;
        } catch (Exception e) {
            log.error("创建日程异常", e);
            return null;
        }
    }

    /**
     * 添加参与者到日程
     */
    private String addAttendeeToEvent(String eventId, String openId) {
        String url = apiBaseUrl + "/calendar/v4/calendars/" + calendarId + "/events/" + eventId + "/attendees";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tenantAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "attendees", java.util.List.of(
                        Map.of(
                                "type", "user",
                                "user_id", openId
                        )
                ),
                "need_notification", true
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = response.getBody();
            log.info("添加参与者API响应: {}", responseBody);

            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                    return "success";
                }
                return "添加失败: " + result.get("msg");
            }
            return "HTTP错误: " + response.getStatusCode() + ", 响应: " + responseBody;
        } catch (Exception e) {
            log.error("添加参与者异常", e);
            return "调用异常: " + e.getMessage();
        }
    }

    /**
     * 获取应用的主日历ID
     */
    private String getPrimaryCalendarId() {
        String url = apiBaseUrl + "/calendar/v4/calendars";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tenantAccessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = response.getBody();
                if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    if (data != null && data.containsKey("calendar_list")) {
                        java.util.List<Map<String, Object>> calendars =
                                (java.util.List<Map<String, Object>>) data.get("calendar_list");
                        if (calendars != null && !calendars.isEmpty()) {
                            return (String) calendars.get(0).get("calendar_id");
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 获取用户信息
     */
    @SuppressWarnings("unchecked")
    public String getUserId(String unionId) {
        ensureToken();

        String url = apiBaseUrl + "/contact/v3/users/" + unionId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tenantAccessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = response.getBody();
                if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    Map<String, Object> user = (Map<String, Object>) data.get("user");
                    return (String) user.get("user_id");
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 创建群组
     *
     * @param groupName 群组名称
     * @return 创建结果
     */
    public String createGroup(String groupName) {
        ensureToken();

        String url = apiBaseUrl + "/im/v1/chats";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tenantAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "name", groupName,
                "chat_mode", "group",
                "chat_type", "private"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = response.getBody();
            log.info("创建群组API响应: {}", responseBody);
            log.info("HTTP状态码: {}", response.getStatusCode());

            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            // 检查 code
            String code = String.valueOf(result.get("code"));
            String msg = String.valueOf(result.get("msg"));
            log.info("API返回 code: {}, msg: {}", code, msg);

            // 检查 data 中是否有群组信息
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null) {
                String chatId = (String) data.get("chat_id");
                log.info("创建的群组 chat_id: {}", chatId);
                if (chatId != null && !chatId.isEmpty()) {
                    return "success";
                }
            }

            // 即使 code 是 0，如果没有 chat_id 也算失败
            return "创建失败: " + msg + " (code=" + code + ")";
        } catch (Exception e) {
            log.error("创建群组异常", e);
            return "调用异常: " + e.getMessage();
        }
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}