package com.example.IntelligentRobot.service.handler;

import com.example.IntelligentRobot.annotation.Command;
import com.example.IntelligentRobot.client.GitHubClient;
import com.example.IntelligentRobot.dto.CommandContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub 仓库查询指令处理器（新框架版本）
 * 指令格式：/repo <owner/repo> - 查看仓库信息
 * 支持别名：/repo <alias> - 通过别名查看仓库信息
 */
@Component
public class RepoCommandHandler {

    private final GitHubClient gitHubClient;

    private static final Pattern REPO_PATTERN =
            Pattern.compile("^([^/]+)/([^\\s]+)");

    /**
     * 仓库别名映射（别名 -> owner/repo）
     * 格式：别名1=owner1/repo1,别名2=owner2/repo2
     */
    @Value("${github.repo-aliases:}")
    private String repoAliasesConfig;

    /**
     * 解析后的别名映射表
     */
    private Map<String, String> aliasMap = new HashMap<>();

    public RepoCommandHandler(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    /**
     * 初始化别名映射表
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        if (repoAliasesConfig != null && !repoAliasesConfig.isEmpty()) {
            String[] aliases = repoAliasesConfig.split(",");
            for (String alias : aliases) {
                String[] parts = alias.trim().split("=");
                if (parts.length == 2) {
                    aliasMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }

    @Command(
        name = "repo",
        description = "查看 GitHub 仓库信息",
        usage = "/repo <owner/repo> 或 /repo <alias>"
    )
    public String handle(CommandContext context) {
        String args = context.getArgs().trim();

        if (args.isEmpty()) {
            StringBuilder usage = new StringBuilder();
            usage.append("❌ 用法：/repo <owner/repo> 或 /repo <alias>\n\n");
            usage.append("📋 示例：\n");
            usage.append("/repo facebook/react\n");
            usage.append("/repo microsoft/vscode\n\n");
            
            if (!aliasMap.isEmpty()) {
                usage.append("💡 已配置的别名：\n");
                for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
                    usage.append("/repo ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
                }
            } else {
                usage.append("💡 查看仓库的详细信息\n");
            }
            
            return usage.toString();
        }

        // 检查是否配置了GitHub Token
        if (!gitHubClient.isConfigured()) {
            return """
                    ⚠️ GitHub 功能未配置

                    请配置 github.token 环境变量
                    当前功能受限
                    """;
        }

        // 先检查是否是别名
        if (aliasMap.containsKey(args)) {
            String repoFull = aliasMap.get(args);
            String[] parts = repoFull.split("/");
            if (parts.length == 2) {
                return gitHubClient.getRepoInfoText(parts[0], parts[1]);
            } else {
                return "❌ 别名配置错误：" + args + " -> " + repoFull + "\n正确格式应为：owner/repo";
            }
        }

        // 如果不是别名，按 owner/repo 格式解析
        Matcher repoMatcher = REPO_PATTERN.matcher(args);
        if (repoMatcher.find()) {
            String owner = repoMatcher.group(1);
            String repo = repoMatcher.group(2);
            return gitHubClient.getRepoInfoText(owner, repo);
        }

        return "❌ 仓库格式错误，应为：owner/repo 或已配置的别名\n示例：/repo facebook/react";
    }
}
