package com.example.intelligentxtsystem.service.handler;

import com.example.intelligentxtsystem.client.FeishuClient;
import com.example.intelligentxtsystem.dto.FeishuSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日程指令处理器
 * 指令格式：/schedule <时间> <事件>
 * 示例：/schedule 2024-01-15 15:00 团队会议
 */
@Component
public class ScheduleHandler implements CommandHandler {

    private final FeishuClient feishuClient;

    private static final Pattern SCHEDULE_PATTERN =
            Pattern.compile("^(?:/schedule|日程)\\s*(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2})\\s+(.+)");

    public ScheduleHandler(FeishuClient feishuClient) {
        this.feishuClient = feishuClient;
    }

    @Override
    public boolean support(String text) {
        return text.startsWith("/schedule") || text.startsWith("日程") || text.startsWith("创建日程");
    }

    @Override
    public String handle(String text, FeishuSender sender) {
        Matcher matcher = SCHEDULE_PATTERN.matcher(text);

        if (!matcher.find()) {
            return """
                    ❌ 用法：/schedule <时间> <事件>
                    
                    📅 示例：
                    /schedule 2024-01-15 15:00 团队周会
                    /schedule 明天 14:00 项目评审
                    
                    💡 时间格式：YYYY-MM-DD HH:mm
                    """;
        }

        String dateTimeStr = matcher.group(1);
        String event = matcher.group(2);

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            if (dateTime.isBefore(LocalDateTime.now())) {
                return "⚠️ 不能创建过去时间的日程";
            }

            // 获取发送者的 open_id
            String userOpenId = sender != null ? sender.getOpenId() : null;

            // 调用飞书日历 API 创建日程（邀请用户参与）
            String result = feishuClient.createCalendarEventWithAttendee(event, dateTime, null, userOpenId);

            if ("success".equals(result)) {
                return String.format("""
                        📅 日程创建成功！
                        
                        📋 事件：%s
                        ⏰ 时间：%s
                        
                        ✅ 已添加到你的飞书日历
                        """, event, dateTimeStr);
            } else {
                return String.format("""
                        ⚠️ 日程创建遇到问题
                        
                        📋 事件：%s
                        ⏰ 时间：%s
                        📌 原因：%s
                        
                        请检查飞书应用权限后重试
                        """, event, dateTimeStr, result);
            }

        } catch (DateTimeParseException e) {
            return "⚠️ 时间格式错误，请使用：YYYY-MM-DD HH:mm";
        }
    }
}
