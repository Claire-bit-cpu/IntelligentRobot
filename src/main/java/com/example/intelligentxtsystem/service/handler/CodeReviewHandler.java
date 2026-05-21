package com.example.intelligentxtsystem.service.handler;

import com.example.intelligentxtsystem.client.GitHubClient;
import com.example.intelligentxtsystem.client.QwenClient;
import com.example.intelligentxtsystem.dto.FeishuSender;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码审查指令处理器
 * 指令格式：/cr <owner/repo> <PR号> - 自动代码审查
 */
@Component
public class CodeReviewHandler implements CommandHandler {

    private final GitHubClient gitHubClient;
    private final QwenClient qwenClient;

    // /cr owner/repo 123
    private static final Pattern CR_PATTERN =
            Pattern.compile("^(?:/cr|审查|代码审查)\\s+([^/]+)/([^\\s]+)\\s+(\\d+)");

    public CodeReviewHandler(GitHubClient gitHubClient, QwenClient qwenClient) {
        this.gitHubClient = gitHubClient;
        this.qwenClient = qwenClient;
    }

    @Override
    public boolean support(String text) {
        return text.startsWith("/cr") || text.startsWith("审查") || text.startsWith("代码审查");
    }

    @Override
    public String handle(String text, FeishuSender sender) {
        // 检查是否配置了GitHub Token
        if (!gitHubClient.isConfigured()) {
            return """
                    ⚠️ 代码审查功能未配置

                    请配置 github.token 环境变量
                    当前功能受限
                    """;
        }

        Matcher matcher = CR_PATTERN.matcher(text);
        if (!matcher.find()) {
            return """
                    ❌ 用法：/cr <owner/repo> <PR号>

                    📋 示例：
                    /cr facebook/react 12345
                    审查 microsoft/vscode 123

                    💡 功能说明：
                    自动分析PR代码，给出审查建议
                    """;
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);
        int prNumber = Integer.parseInt(matcher.group(3));

        return """
                🔍 正在分析 PR #%d ...

                📦 仓库：%s/%s

                ⚠️ 注意：完整代码审查需要配置代码访问权限

                💡 当前显示PR基本信息：
                """.formatted(prNumber, owner, repo) + "\n\n" +
                gitHubClient.getPRInfo(owner, repo, prNumber);
    }
}
