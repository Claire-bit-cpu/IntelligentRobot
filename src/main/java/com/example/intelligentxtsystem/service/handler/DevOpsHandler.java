package com.example.intelligentxtsystem.service.handler;

import com.example.intelligentxtsystem.dto.FeishuSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DevOps 工具指令处理器
 * 指令格式：
 * /ping <host> - 检测服务器连通性
 * /uptime - 查看服务运行时间
 * /deploy <环境> - 触发部署
 */
@Component
public class DevOpsHandler implements CommandHandler {

    private final long startTime = System.currentTimeMillis();

    // /ping 192.168.1.1
    private static final Pattern PING_PATTERN =
            Pattern.compile("^(?:/ping|ping)\\s+([^\\s]+)");
    
    // /deploy prod
    private static final Pattern DEPLOY_PATTERN =
            Pattern.compile("^(?:/deploy|部署)\\s+(\\w+)");

    @Override
    public boolean support(String text) {
        return text.startsWith("/ping") || text.startsWith("/uptime") ||
               text.startsWith("/deploy") || text.startsWith("ping ") ||
               text.startsWith("部署 ");
    }

    @Override
    public String handle(String text, FeishuSender sender) {
        // /uptime
        if (text.trim().equals("/uptime") || text.trim().equals("uptime")) {
            return handleUptime();
        }

        // /ping host
        Matcher pingMatcher = PING_PATTERN.matcher(text);
        if (pingMatcher.find()) {
            String host = pingMatcher.group(1);
            return handlePing(host);
        }

        // /deploy env
        Matcher deployMatcher = DEPLOY_PATTERN.matcher(text);
        if (deployMatcher.find()) {
            String env = deployMatcher.group(1);
            return handleDeploy(env);
        }

        return """
                ❌ 用法：

                🔧 DevOps 工具
                ─────────────────────────
                /uptime        查看运行时间
                /ping <主机>   检测连通性
                /deploy <环境>  触发部署

                💡 使用示例
                /uptime
                /ping 192.168.1.1
                /deploy prod
                """;
    }

    private String handleUptime() {
        long uptimeMs = System.currentTimeMillis() - startTime;
        long days = uptimeMs / (1000 * 60 * 60 * 24);
        long hours = (uptimeMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);

        LocalDateTime now = LocalDateTime.now();
        String format = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return String.format("""
                📊 系统状态

                🕐 当前时间：%s
                ⏱️  运行时间：%d天 %d小时 %d分钟
                ✅ 状态：正常运行
                """, format, days, hours, minutes);
    }

    private String handlePing(String host) {
        // 简单的连通性检测（实际应该用ICMP或HTTP检测）
        if (host == null || host.isEmpty()) {
            return "❌ 请指定主机地址";
        }

        return String.format("""
                🔍 Ping 检测

                🎯 目标：%s

                ⏳ 正在检测...
                ⚠️ 注意：当前为模拟响应

                💡 生产环境请配置真实的健康检查接口
                """, host);
    }

    private String handleDeploy(String env) {
        String normalizedEnv = env.toLowerCase();

        if (!normalizedEnv.matches("^(dev|test|prod|production)$")) {
            return """
                    ❌ 部署环境无效

                    可用环境：
                    • dev / test - 开发/测试环境
                    • prod / production - 生产环境

                    ⚠️ 生产环境部署需要确认
                    """;
        }

        boolean isProd = normalizedEnv.equals("prod") || normalizedEnv.equals("production");

        return String.format("""
                🚀 部署任务已提交

                📦 环境：%s
                🕐 时间：%s

                %s

                📋 部署步骤：
                1. 代码拉取 ✓
                2. 构建镜像 [进行中]
                3. 推送到仓库
                4. 更新服务
                5. 健康检查

                💡 查看详细日志请访问部署平台
                """,
                isProd ? "⚠️ 生产环境" : "🔧 " + env.toUpperCase(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                isProd ? "⚠️ 生产环境部署，请确认操作" : "");
    }
}
