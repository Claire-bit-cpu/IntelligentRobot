package com.example.intelligentxtsystem.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 通义千问 API 客户端
 * 用于翻译等功能
 */
@Component
public class QwenClient {

    private static final Logger logger = LoggerFactory.getLogger(QwenClient.class);

    @Value("${qianyu.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    /**
     * 使用通义千问进行翻译（中英互译）
     *
     * @param text 待翻译文本
     * @return 翻译结果
     */
    public String translate(String text) {
        logger.info("QwenClient.translate() 被调用: " + text);
        
        // 构建提示词
        String prompt = buildTranslatePrompt(text);
        logger.info("翻译 Prompt: " + prompt);

        try {
            // 调用通义千问 API，直接获取响应体
            var response = callQwen(prompt);
            logger.info("API 响应体: " + response);

            // 解析响应 - 响应直接包含 output 字段
            if (response != null && response.containsKey("output")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                String result = (String) output.get("text");
                logger.info("翻译结果: " + result);
                return result;
            }

            logger.warn("响应格式异常，未找到 output 字段");
            return "⚠️ 翻译结果解析失败";
        } catch (Exception e) {
            logger.warn("翻译异常: " + e.getMessage());
            e.printStackTrace();
            return "⚠️ 翻译服务暂时不可用：" + e.getMessage();
        }
    }

    /**
     * 构建翻译提示词
     */
    private String buildTranslatePrompt(String text) {
        boolean isEnglish = text.matches("^[a-zA-Z].*");

        if (isEnglish) {
            return String.format("""
                    请将以下英文翻译成中文，只返回翻译结果，不要任何解释：
                    
                    %s
                    """, text);
        } else {
            return String.format("""
                    请将以下中文翻译成英文，只返回翻译结果，不要任何解释：
                    
                    %s
                    """, text);
        }
    }

    /**
     * 调用通义千问 API
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callQwen(String prompt) {
        logger.info("开始调用通义千问 API");

        Map<String, Object> body = Map.of(
                "model", "qwen-turbo",
                "input", Map.of("prompt", prompt)
        );

        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        var entity = new org.springframework.http.HttpEntity<>(body, headers);

        logger.info("API 请求 URL: " + API_URL);
        logger.info("API 请求体: " + body);

        var response = restTemplate.postForEntity(API_URL, entity, Map.class);

        logger.info("API 响应状态: " + response.getStatusCode());

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }

        return null;
    }

    /**
     * 使用通义千问回答问题
     *
     * @param question 问题
     * @return 回答
     */
    public String answerQuestion(String question) {
        logger.info("QwenClient.answerQuestion() 被调用: " + question);

        String prompt = buildQuestionPrompt(question);
        logger.info("问答 Prompt: " + prompt);

        try {
            var response = callQwen(prompt);
            logger.info("API 响应体: " + response);

            if (response != null && response.containsKey("output")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                String result = (String) output.get("text");
                logger.info("问答结果: " + result);
                return result;
            }

            logger.warn("响应格式异常，未找到 output 字段");
            return "抱歉，我暂时无法回答这个问题";
        } catch (Exception e) {
            logger.warn("问答异常: " + e.getMessage());
            return "抱歉，服务暂时不可用，请稍后再试";
        }
    }

    /**
     * 构建问答提示词
     */
    private String buildQuestionPrompt(String question) {
        return String.format("""
                你是一个智能助手，请用简洁清晰的方式回答以下问题。如果问题不清晰，请说明需要更多信息。

                问题：%s
                """, question);
    }
}
