package com.example.intelligentxtsystem.service;

import com.example.intelligentxtsystem.dto.CommandContext;
import com.example.intelligentxtsystem.dto.FeishuSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息分发器
 * 使用 CommandRegistry 进行指令分发
 */
@Service
public class MessageDispatcher {
    
    private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);
    
    private final CommandRegistry commandRegistry;
    
    /**
     * 匹配 /指令名 或 指令名 的正则
     */
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^(?:/)?(\\w+)(.*)$");
    
    public MessageDispatcher(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }
    
    @PostConstruct
    public void init() {
        log.info("MessageDispatcher 初始化完成，已加载 {} 个指令", 
            commandRegistry.getAllCommands().size());
    }
    
    /**
     * 分发消息
     * @param text 消息文本
     * @param sender 发送者信息
     * @param chatId 群聊 ID
     * @return 处理结果
     */
    public String dispatch(String text, FeishuSender sender, String chatId) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        String trimmedText = text.trim();
        
        // 匹配指令
        Matcher matcher = COMMAND_PATTERN.matcher(trimmedText);
        if (!matcher.find()) {
            log.debug("无法解析指令: {}", trimmedText);
            return null;
        }
        
        String commandName = matcher.group(1).toLowerCase();
        String args = matcher.group(2).trim();
        
        // 检查指令是否存在
        if (!commandRegistry.hasCommand(commandName)) {
            log.debug("未知指令: /{}", commandName);
            return generateUnknownCommandHelp(commandName);
        }
        
        // 创建指令上下文
        CommandContext context = new CommandContext();
        context.setCommandName(commandName);
        context.setArgs(args);
        context.setSender(sender);
        context.setRawMessage(trimmedText);
        
        // 执行指令
        try {
            log.info("执行指令: /{}，参数: {}", commandName, args);
            Object result = commandRegistry.execute(commandName, context);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.error("执行指令失败: /{}", commandName, e);
            return "❌ 执行指令失败: " + e.getMessage();
        }
    }
    
    /**
     * 生成未知指令的帮助信息
     */
    private String generateUnknownCommandHelp(String commandName) {
        return String.format(
            "❌ 未知指令: /%s\n\n💡 使用 /help 查看所有可用指令",
            commandName
        );
    }
}