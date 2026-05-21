package com.example.intelligentxtsystem.service.handler;

import com.example.intelligentxtsystem.client.QwenClient;
import com.example.intelligentxtsystem.dto.FeishuSender;
import org.springframework.stereotype.Component;

/**
 * 知识搜索指令处理器
 * 指令格式：/search <问题> 或 搜索 <问题>
 * 使用通义千问进行智能问答
 */
@Component
public class SearchHandler implements CommandHandler {

    private final QwenClient qwenClient;

    public SearchHandler(QwenClient qwenClient) {
        this.qwenClient = qwenClient;
    }

    @Override
    public boolean support(String text) {
        return text.startsWith("/search") || text.startsWith("搜索") || text.startsWith("查询");
    }

    @Override
    public String handle(String text, FeishuSender sender) {
        String query = text
                .replaceAll("^(/search|搜索|查询)\\s*", "")
                .trim();

        if (query.isEmpty()) {
            return """
                    ❌ 用法：/search <问题>
                    
                    📋 示例：
                    /search 如何创建项目
                    搜索 Java数组怎么用
                    
                    💡 可以询问任何技术或业务问题
                    """;
        }

        if (query.length() > 1000) {
            return "⚠️ 问题长度不能超过1000个字符";
        }

        try {
            String answer = qwenClient.answerQuestion(query);
            return String.format("""
                    🔍 问题：%s
                    
                    💡 回答：
                    %s
                    """, query, answer);
        } catch (Exception e) {
            return "⚠️ 搜索服务暂时不可用，请稍后再试";
        }
    }
}
