package com.example.intelligentxtsystem.service.handler;

import com.example.intelligentxtsystem.client.GitHubClient;
import com.example.intelligentxtsystem.client.GitLabClient;
import com.example.intelligentxtsystem.client.JiraClient;
import com.example.intelligentxtsystem.client.MonitorClient;
import com.example.intelligentxtsystem.dto.FeishuSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DevOps 工具指令处理器
 * 指令格式：
 * /ping <host> - 检测服务器连通性
 * /uptime - 查看服务运行时间
 * /deploy <环境> - 触发部署流程（GitHub Actions）
 * /jira <任务编号> - 查询 JIRA 任务
 * /jira create <项目> <标题> - 创建 JIRA 任务
 * /monitor <服务名> - 查询服务健康状态与错误率
 * /github workflow <仓库> <工作流> <分支> - 触发 GitHub Actions 工作流
 * /github status <仓库> <run-id> - 查询 GitHub Actions 运行状态
 */
@Component
public class DevOpsHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(DevOpsHandler.class);

    private final long startTime = System.currentTimeMillis();

    @Value("${devops.ping-timeout-ms:5000}")
    private int pingTimeoutMs;

    @Value("${devops.http-timeout-ms:10000}")
    private int httpTimeoutMs;

    @Autowired(required = false)
    private GitHubClient gitHubClient;

    @Autowired(required = false)
    private GitLabClient gitLabClient;

    @Autowired(required = false)
    private JiraClient jiraClient;

    @Autowired(required = false)
    private MonitorClient monitorClient;

    /**
     * Spring 环境对象，用于读取配置
     */
    @Autowired
    private Environment environment;

    /**
     * 部署环境映射配置
     * 格式：环境名 = owner/repo:工作流文件名:分支名
     * 示例：dev: Claire-bit-cpu/Test:deploy-dev.yml:develop
     */
    private Map<String, String> deployConfig;

    @PostConstruct
    public void init() {
        deployConfig = loadDeployConfig();
    }

    /**
     * 从 Environment 加载部署配置
     * 手动解析 github.deploy 配置（避免 @Value 类型转换问题）
     */
    private Map<String, String> loadDeployConfig() {
        Map<String, String> configMap = new java.util.HashMap<>();
        
        // 获取配置字符串（格式：dev:xxx:xxx:xxx,test:xxx:xxx:xxx）
        String configStr = environment.getProperty("github.deploy");
        
        if (configStr == null || configStr.isEmpty()) {
            log.warn("未找到 github.deploy 配置，将使用默认配置");
            return configMap;
        }

        log.info("加载部署配置: {}", configStr);
        
        // 按逗号分隔多个配置
        String[] pairs = configStr.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                configMap.put(key, value);
                log.debug("加载部署配置: {} = {}", key, value);
            }
        }
        
        return configMap;
    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // /ping 192.168.1.1 或 /ping http://example.com 或 /ping [IPv6]
    private static final Pattern PING_PATTERN =
            Pattern.compile("^(?:/ping|ping)\\s+(.+?)(?:\\s|$)");

    // /deploy prod 或 /deploy jenkins/job-name
    private static final Pattern DEPLOY_PATTERN =
            Pattern.compile("^(?:/deploy|部署)\\s+([^\\s]+)");

    // /jira PROJ-123 或 /jira create PROJ 标题
    private static final Pattern JIRA_PATTERN =
            Pattern.compile("^(?:/jira|jira)\\s+(.+)");

    // /monitor service-name 或 /monitor <service>
    private static final Pattern MONITOR_PATTERN =
            Pattern.compile("^(?:/monitor|监控)\\s+([^\\s]+)");

    // /gitlab pipeline <项目> <分支> 或 /gitlab status <项目> <pipeline-id>
    private static final Pattern GITLAB_PATTERN =
            Pattern.compile("^(?:/gitlab|gitlab)\\s+(.+)");

    // /github workflow <仓库> <工作流> <分支> 或 /github status <仓库> <run-id>
    private static final Pattern GITHUB_PATTERN =
            Pattern.compile("^(?:/github|github)\\s+(.+)");

    @Override
    public boolean support(String text) {
        return text.startsWith("/ping") || text.startsWith("/uptime") ||
               text.startsWith("/deploy") || text.startsWith("ping ") ||
               text.startsWith("部署 ") || text.startsWith("/jira") ||
               text.startsWith("jira ") || text.startsWith("/monitor") ||
               text.startsWith("监控 ") || text.startsWith("/gitlab") ||
               text.startsWith("gitlab ") || text.startsWith("/github") ||
               text.startsWith("github ");
    }

    @Override
    public String handle(String text, FeishuSender sender, String chatId) {
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
            return handleDeploy(env, sender);
        }

        // /jira task
        Matcher jiraMatcher = JIRA_PATTERN.matcher(text);
        if (jiraMatcher.find()) {
            String jiraCmd = jiraMatcher.group(1);
            return handleJira(jiraCmd, sender);
        }

        // /monitor service
        Matcher monitorMatcher = MONITOR_PATTERN.matcher(text);
        if (monitorMatcher.find()) {
            String service = monitorMatcher.group(1);
            return handleMonitor(service);
        }

        // /gitlab command
        Matcher gitlabMatcher = GITLAB_PATTERN.matcher(text);
        if (gitlabMatcher.find()) {
            String gitlabCmd = gitlabMatcher.group(1);
            return handleGitLab(gitlabCmd, sender);
        }

        // /github command
        Matcher githubMatcher = GITHUB_PATTERN.matcher(text);
        if (githubMatcher.find()) {
            String githubCmd = githubMatcher.group(1);
            return handleGitHub(githubCmd, sender);
        }

        return buildHelpText();
    }

    private String handleUptime() {
        long uptimeMs = System.currentTimeMillis() - startTime;
        long days = uptimeMs / (1000 * 60 * 60 * 24);
        long hours = (uptimeMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (uptimeMs % (1000 * 60)) / 1000;

        String now = LocalDateTime.now(ZONE).format(FORMATTER);

        return String.format("""
                📊 系统状态

                🕐 当前时间：%s
                ⏱  运行时间：%d天 %d小时 %d分钟 %d秒
                ✅ 状态：正常运行
                """, now, days, hours, minutes, seconds);
    }

    private String handlePing(String host) {
        if (host == null || host.isEmpty()) {
            return "❌ 请指定主机地址";
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("🔍 Ping 检测\n\n🎯 目标：%s\n\n", host));

        // 清理主机名（移除 http:// 前缀和路径）
        String cleanHost = host.replaceAll("^https?://", "").replaceAll("/.*$", "");
        // 去除 IPv6 地址的方括号（保留接口标识 %15 等）
        cleanHost = cleanHost.replaceAll("^\\[(.+)\\]$", "$1");

        // 判断是否为 IP 地址（IPv4 或 IPv6）
        boolean isIpAddress = isIpAddress(cleanHost);

        // 1. ICMP Ping（域名/IP 连通性）
        try {
            long start = System.currentTimeMillis();
            boolean reachable = InetAddress.getByName(cleanHost).isReachable(pingTimeoutMs);
            long pingTime = System.currentTimeMillis() - start;

            if (reachable) {
                result.append(String.format("📡 ICMP：✅ 可达（%dms）\n", pingTime));
            } else {
                result.append("📡 ICMP：❌ 不可达\n");
            }
        } catch (Exception e) {
            result.append("📡 ICMP：❌ 解析失败（").append(e.getMessage()).append("）\n");
        }

        // 2. TCP 端口检测（如果指定了端口，如 host:port）
        if (host.contains(":") && !isLikelyIPv6WithoutPort(cleanHost)) {
            String[] parts = host.replaceAll("^\\[", "").replaceAll("\\].*$", "").split(":", 2);
            if (parts.length == 2) {
                try {
                    int port = Integer.parseInt(parts[1].replaceAll(".*:", "").replaceAll("/.*$", ""));
                    String tcpHost = parts[0].replaceAll("^https?://", "");
                    boolean tcpReachable = testTcpPort(tcpHost, port);
                    result.append(String.format("🔌 TCP %d：%s\n", port, tcpReachable ? "✅ 端口开放" : "❌ 端口关闭"));
                } catch (NumberFormatException ignored) {
                    // 端口解析失败，跳过
                }
            }
        }

        // 3. HTTP 检测（仅当明确指定 http:// 或 https:// 时进行）
        if (host.startsWith("http://") || host.startsWith("https://")) {
            try {
                // 正确处理 IPv6 地址的 URL
                String url = host;
                if (cleanHost.contains(":") && !cleanHost.contains("[")) {
                    // IPv6 地址但没有方括号，需要添加
                    url = host.replaceFirst("(https?://)([^\\[].*?)(:|/|$)", "$1[$2]$3");
                }

                long start = System.currentTimeMillis();
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(httpTimeoutMs);
                conn.setReadTimeout(httpTimeoutMs);
                // 只获取响应码，不读body
                int code = conn.getResponseCode();
                long httpTime = System.currentTimeMillis() - start;
                conn.disconnect();

                result.append(String.format("🌐 HTTP：✅ 状态码 %d（%dms）\n", code, httpTime));
            } catch (Exception e) {
                result.append("🌐 HTTP：❌ 连接失败（").append(e.getMessage()).append("）\n");
            }
        } else if (!isIpAddress) {
            // 不是 IP 地址，尝试 HTTPS 检测（域名）
            try {
                String url = "https://" + cleanHost;
                long start = System.currentTimeMillis();
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(httpTimeoutMs);
                conn.setReadTimeout(httpTimeoutMs);
                int code = conn.getResponseCode();
                long httpTime = System.currentTimeMillis() - start;
                conn.disconnect();

                result.append(String.format("🌐 HTTP：✅ 状态码 %d（%dms）\n", code, httpTime));
            } catch (Exception e) {
                result.append("🌐 HTTP：❌ 连接失败（").append(e.getMessage()).append("）\n");
            }
        }

        result.append("\n🕐 检测时间：").append(LocalDateTime.now(ZONE).format(FORMATTER));
        return result.toString();
    }

    /**
     * 判断字符串是否为 IP 地址（IPv4 或 IPv6）
     */
    private boolean isIpAddress(String host) {
        // 移除接口标识（IPv6 链路本地地址的 %15 等）
        String cleaned = host.replaceAll("%\\d+$", "");
        // 移除端口号
        cleaned = cleaned.replaceAll(":\\d+$", "");

        // 检查是否是 IPv4 地址
        if (cleaned.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            return true;
        }

        // 检查是否是 IPv6 地址
        if (cleaned.contains(":")) {
            try {
                InetAddress.getByName(cleaned);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * 判断是否是 IPv6 地址（没有端口的情况）
     */
    private boolean isLikelyIPv6WithoutPort(String host) {
        // 统计冒号数量，IPv6 地址至少有 2 个冒号
        return host.contains(":") && host.split(":").length >= 2;
    }

    /**
     * TCP 端口检测
     */
    private boolean testTcpPort(String host, int port) {
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), pingTimeoutMs);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String handleDeploy(String env, FeishuSender sender) {
        String normalizedEnv = env.toLowerCase();
        String operator = sender != null ? sender.getOpenId() : "系统";
        String now = LocalDateTime.now(ZONE).format(FORMATTER);

        // 如果配置了 GitHub，实际触发部署
        if (gitHubClient != null && gitHubClient.isConfigured()) {
            try {
                // 根据环境映射到 GitHub 仓库和工作流
                DeployConfig config = getGitHubDeployConfig(normalizedEnv);
                String result = gitHubClient.triggerWorkflow(
                        config.owner(), 
                        config.repo(), 
                        config.workflowId(), 
                        config.branch(), 
                        null
                );

                return String.format("""
                        🚀 部署已触发

                        📦 环境：%s
                        📂 仓库：%s/%s
                        🔧 工作流：%s
                        👤 操作者：%s
                        🕐 时间：%s

                        %s

                        🔗 GitHub Actions: %s/%s/actions
                        """, normalizedEnv, config.owner(), config.repo(), 
                           config.workflowId(), operator, now, result,
                           config.owner(), config.repo());
            } catch (Exception e) {
                log.error("GitHub Actions 部署失败", e);
                return String.format("""
                        ❌ 部署失败

                        📦 环境：%s
                        👤 操作者：%s

                        错误：%s

                        💡 请检查 GitHub 配置
                        """, normalizedEnv, operator, e.getMessage());
            }
        }

        // 模拟模式
        if (!normalizedEnv.matches("^(dev|test|staging|prod|production)$")) {
            return buildDeployHelp();
        }

        boolean isProd = normalizedEnv.equals("prod") || normalizedEnv.equals("production");

        if (isProd) {
            return String.format("""
                    ⚠️ 生产环境部署确认

                    📦 环境：production
                    👤 操作者：%s
                    🕐 时间：%s

                    ❗ 生产环境部署需要额外确认！
                    当前为模拟模式，未执行实际部署操作。

                    如需接入真实部署，请配置 github.token 并启用 GitHub 集成
                    """, operator, now);
        }

        String envLabel = switch (normalizedEnv) {
            case "dev" -> "开发环境";
            case "test" -> "测试环境";
            case "staging" -> "预发布环境";
            default -> normalizedEnv;
        };

        return String.format("""
                🚀 模拟部署流程

                📦 环境：%s（%s）
                👤 操作者：%s
                🕐 时间：%s

                📋 模拟步骤：
                1. 代码拉取 ✓
                2. 构建打包 ✓
                3. 部署服务 ✓
                4. 健康检查 ✓

                ✅ 模拟部署完成

                💡 当前为模拟模式。
                如需接入真实部署，请配置 GitHub Actions 集成。
                """, normalizedEnv, envLabel, operator, now);
    }

    /**
     * 部署配置记录
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param workflowId 工作流 ID 或文件名
     * @param branch 分支名称
     */
    private record DeployConfig(String owner, String repo, String workflowId, String branch) {}

    /**
     * 根据环境获取 GitHub 部署配置
     * 优先从 application.yaml 的 github.deploy 配置读取
     * 格式：环境名 = owner/repo:工作流文件名:分支名
     * 示例：dev: Claire-bit-cpu/Test:deploy-dev.yml:develop
     */
    private DeployConfig getGitHubDeployConfig(String env) {
        // 从配置文件读取
        if (deployConfig != null && deployConfig.containsKey(env)) {
            String config = deployConfig.get(env);
            try {
                String[] parts = config.split(":");
                if (parts.length >= 3) {
                    String[] repoParts = parts[0].split("/", 2);
                    if (repoParts.length == 2) {
                        return new DeployConfig(repoParts[0], repoParts[1], parts[1], parts[2]);
                    }
                }
                log.warn("部署配置格式错误: {} = {}", env, config);
            } catch (Exception e) {
                log.error("解析部署配置失败: {}", config, e);
            }
        }
        
        // 默认配置（兼容旧版本）
        return switch (env) {
            case "dev" -> new DeployConfig("Claire-bit-cpu", "Test", "deploy-dev.yml", "develop");
            case "test" -> new DeployConfig("Claire-bit-cpu", "Test", "deploy-test.yml", "test");
            case "staging" -> new DeployConfig("Claire-bit-cpu", "Test", "deploy-staging.yml", "staging");
            case "prod", "production" -> new DeployConfig("Claire-bit-cpu", "Test", "deploy-prod.yml", "main");
            default -> new DeployConfig("Claire-bit-cpu", "Test", "deploy-" + env + ".yml", "main");
        };
    }

    private String handleJira(String jiraCmd, FeishuSender sender) {
        // 检查 JIRA 客户端是否可用
        if (jiraClient == null) {
            return """
                    ⚠️ JIRA 客户端未初始化

                    请检查配置：
                    • jira.enabled - 设置为 true
                    • 确保 JiraClient 已正确注入
                    """;
        }

        // 检查是否处于本地降级模式
        if (jiraClient.isLocalMode()) {
            log.info("JIRA 处于本地降级模式，使用本地文件存储任务");
            // 不返回错误，继续执行（会触发本地降级逻辑）
        } else if (!jiraClient.isEnabled()) {
            return """
                    ⚠️ JIRA 集成未启用

                    请配置以下环境变量：
                    • jira.url - JIRA 地址
                    • jira.username - 用户名（邮箱）
                    • jira.api-token - API Token
                    • jira.enabled - 设置为 true

                    💡 获取 API Token：https://id.atlassian.com/manage-profile/security/api-tokens
                    """;
        }

        // /jira create <项目> <标题>
        if (jiraCmd.startsWith("create ")) {
            String[] parts = jiraCmd.substring(7).split("\\s+", 2);
            if (parts.length < 2) {
                return "❌ 用法：/jira create <项目KEY> <标题>\n示例：/jira create PROJ 修复登录Bug";
            }
            String projectKey = parts[0];
            String summary = parts[1];
            return jiraClient.createBug(projectKey, summary, "");
        }

        // /jira search <JQL>
        if (jiraCmd.startsWith("search ")) {
            String jql = jiraCmd.substring(7);
            return jiraClient.searchIssues(jql, 10);
        }

        // /jira <任务编号> - 查询任务
        String issueKey = jiraCmd.trim();
        if (issueKey.matches("^[A-Z]+-\\d+$")) {
            return jiraClient.getIssue(issueKey);
        }

        return """
                ❌ JIRA 命令格式错误

                📋 可用命令：
                • /jira <任务编号> - 查询任务
                • /jira create <项目> <标题> - 创建Bug工单
                • /jira search <JQL> - 搜索任务

                💡 示例：
                /jira PROJ-123
                /jira create PROJ 修复登录Bug
                /jira search assignee=currentUser()
                """;
    }

    private String handleMonitor(String service) {
        if (monitorClient == null) {
            return """
                    ⚠️ 监控集成未启用

                    请配置以下环境变量：
                    • prometheus.url - Prometheus 地址
                    • prometheus.enabled - 设置为 true
                    • grafana.url - Grafana 地址（可选）

                    💡 可获取服务健康状态、错误率、请求速率等指标
                    """;
        }

        if (!monitorClient.isPrometheusEnabled()) {
            return "⚠️ Prometheus 集成未启用，请配置 prometheus.enabled=true";
        }

        // 获取服务综合指标
        return monitorClient.getServiceMetrics(service);
    }

    private String buildHelpText() {
        return """
                🔧 DevOps 工具

                📡 基础工具
                • /uptime - 查看运行时间
                • /ping <主机> - 检测连通性

                🚀 部署工具
                • /deploy <环境> - 触发部署
                • 环境：dev, test, staging, prod

                📋 JIRA 工具
                • /jira <任务编号> - 查询任务
                • /jira create <项目> <标题> - 创建Bug工单
                • /jira search <JQL> - 搜索任务

                📊 监控工具
                • /monitor <服务名> - 查询服务健康状态

                💡 使用示例
                /uptime
                /ping baidu.com
                /deploy test
                /jira PROJ-123
                /jira create PROJ 修复登录Bug
                /monitor my-service
                """;
    }

    private String buildDeployHelp() {
        return """
                ❌ 部署环境无效

                📦 可用环境：
                • dev - 开发环境
                • test - 测试环境
                • staging - 预发布环境
                • prod / production - 生产环境

                💡 示例：
                /deploy test
                /deploy prod

                🔧 高级用法（直接触发 GitHub Actions）：
                /github workflow <仓库> <工作流文件> <分支>
                /github status <仓库> <run-id>
                """;
    }

    private String handleGitLab(String gitlabCmd, FeishuSender sender) {
        if (gitLabClient == null || !gitLabClient.isEnabled()) {
            return """
                    ⚠️ GitLab 集成未启用

                    请配置以下环境变量：
                    • gitlab.token - GitLab Private Token
                    • gitlab.enabled - 设置为 true

                    💡 获取 Token：GitLab → User Settings → Access Tokens
                    """;
        }

        // /gitlab pipeline <项目> <分支>
        if (gitlabCmd.startsWith("pipeline ")) {
            String[] parts = gitlabCmd.substring(9).split("\\s+", 2);
            if (parts.length < 2) {
                return "❌ 用法：/gitlab pipeline <项目ID或路径> <分支>\n示例：/gitlab pipeline mygroup/myproject main";
            }
            String projectId = parts[0];
            String ref = parts[1];
            try {
                return gitLabClient.triggerPipeline(projectId, ref, null);
            } catch (Exception e) {
                log.error("GitLab 触发流水线失败", e);
                return "❌ 触发失败: " + e.getMessage();
            }
        }

        // /gitlab status <项目> <pipeline-id>
        if (gitlabCmd.startsWith("status ")) {
            String[] parts = gitlabCmd.substring(7).split("\\s+", 2);
            if (parts.length < 2) {
                return "❌ 用法：/gitlab status <项目ID> <pipeline-id>\n示例：/gitlab status mygroup/myproject 123";
            }
            String projectId = parts[0];
            try {
                int pipelineId = Integer.parseInt(parts[1]);
                return gitLabClient.getPipelineStatus(projectId, pipelineId);
            } catch (NumberFormatException e) {
                return "❌ Pipeline ID 必须是数字";
            }
        }

        // /gitlab list <项目>
        if (gitlabCmd.startsWith("list ")) {
            String projectId = gitlabCmd.substring(5).trim();
            return gitLabClient.listPipelines(projectId, 10);
        }

        // /gitlab cancel <项目> <pipeline-id>
        if (gitlabCmd.startsWith("cancel ")) {
            String[] parts = gitlabCmd.substring(7).split("\\s+", 2);
            if (parts.length < 2) {
                return "❌ 用法：/gitlab cancel <项目ID> <pipeline-id>";
            }
            String projectId = parts[0];
            try {
                int pipelineId = Integer.parseInt(parts[1]);
                return gitLabClient.cancelPipeline(projectId, pipelineId);
            } catch (NumberFormatException e) {
                return "❌ Pipeline ID 必须是数字";
            }
        }

        return """
                ❌ GitLab 命令格式错误

                🚀 可用命令：
                • /gitlab pipeline <项目> <分支> - 触发流水线
                • /gitlab status <项目> <pipeline-id> - 查询流水线状态
                • /gitlab list <项目> - 列出最近流水线
                • /gitlab cancel <项目> <pipeline-id> - 取消流水线

                💡 示例：
                /gitlab pipeline mygroup/myproject main
                /gitlab status mygroup/myproject 123
                /gitlab list mygroup/myproject
                """;
    }

    private String buildGitLabHelp() {
        return """
                🚀 GitLab CI/CD 工具

                📋 可用命令：
                • /gitlab pipeline <项目> <分支> - 触发流水线
                • /gitlab status <项目> <pipeline-id> - 查询状态
                • /gitlab list <项目> - 列出最近流水线
                • /gitlab cancel <项目> <pipeline-id> - 取消流水线

                💡 项目可以是：
                • 项目 ID（数字）
                • URL 编码路径（如 mygroup%2Fmyproject）
                • 或直接路径（如 mygroup/myproject）

                📝 示例：
                /gitlab pipeline 12345 main
                /gitlab pipeline mygroup/myproject main
                /gitlab status mygroup/myproject 123
                """;
    }

    private String handleGitHub(String githubCmd, FeishuSender sender) {
        if (gitHubClient == null || !gitHubClient.isConfigured()) {
            return """
                    ⚠️ GitHub 集成未启用

                    请配置以下环境变量：
                    • github.token - GitHub Personal Access Token
                    • github.api-url - GitHub API 地址（可选，默认 https://api.github.com）

                    💡 获取 Token：GitHub → Settings → Developer settings → Personal access tokens
                    """;
        }

        // /github workflow <owner/repo> <工作流文件> <分支>
        if (githubCmd.startsWith("workflow ")) {
            String[] parts = githubCmd.substring(9).split("\\s+", 3);
            if (parts.length < 3) {
                return "❌ 用法：/github workflow <owner/repo> <工作流文件> <分支>\n示例：/github workflow Claire-bit-cpu/Test deploy-dev.yml develop";
            }
            String[] repoParts = parts[0].split("/", 2);
            if (repoParts.length < 2) {
                return "❌ 仓库格式错误，应为：owner/repo";
            }
            String owner = repoParts[0];
            String repo = repoParts[1];
            String workflowId = parts[1];
            String ref = parts[2];
            try {
                String result = gitHubClient.triggerWorkflow(owner, repo, workflowId, ref, null);
                return String.format("""
                        🚀 GitHub Actions 工作流已触发

                        📂 仓库：%s/%s
                        🔧 工作流：%s
                        🌿 分支：%s
                        👤 操作者：%s
                        🕐 时间：%s

                        %s

                        🔗 查看详情：https://github.com/%s/%s/actions
                        """, owner, repo, workflowId, ref, 
                           sender != null ? sender.getOpenId() : "系统",
                           LocalDateTime.now(ZONE).format(FORMATTER),
                           result, owner, repo);
            } catch (Exception e) {
                log.error("GitHub Actions 触发失败", e);
                return "❌ 触发失败: " + e.getMessage();
            }
        }

        // /github status <owner/repo> <run-id>
        if (githubCmd.startsWith("status ")) {
            String[] parts = githubCmd.substring(7).split("\\s+", 2);
            if (parts.length < 2) {
                return "❌ 用法：/github status <owner/repo> <run-id>\n示例：/github status Claire-bit-cpu/Test 123456";
            }
            String[] repoParts = parts[0].split("/", 2);
            if (repoParts.length < 2) {
                return "❌ 仓库格式错误，应为：owner/repo";
            }
            String owner = repoParts[0];
            String repo = repoParts[1];
            try {
                int runId = Integer.parseInt(parts[1]);
                Map<String, Object> run = gitHubClient.getWorkflowRun(owner, repo, runId);
                if (run == null) {
                    return "❌ 获取工作流运行状态失败";
                }
                String status = (String) run.get("status");
                String conclusion = (String) run.get("conclusion");
                String htmlUrl = (String) run.get("html_url");
                Object headBranchObj = run.get("head_branch");
                String headBranch = headBranchObj != null ? headBranchObj.toString() : "N/A";
                Object headCommitObj = run.get("head_sha");
                String headCommit = headCommitObj != null ? headCommitObj.toString() : "N/A";

                return String.format("""
                        🔍 GitHub Actions 运行状态

                        📂 仓库：%s/%s
                        🆔 运行 ID：%d
                        🌿 分支：%s
                        📝 提交：%s
                        📊 状态：%s
                        🎯 结果：%s

                        🔗 查看详情：%s
                        """, owner, repo, runId, headBranch, 
                           headCommit != null ? headCommit.substring(0, 8) : "N/A",
                           status, 
                           conclusion != null ? conclusion : "进行中",
                           htmlUrl);
            } catch (NumberFormatException e) {
                return "❌ 运行 ID 必须是数字";
            }
        }

        // /github list <owner/repo> [工作流文件]
        if (githubCmd.startsWith("list ")) {
            String[] parts = githubCmd.substring(5).split("\\s+", 2);
            String[] repoParts = parts[0].split("/", 2);
            if (repoParts.length < 2) {
                return "❌ 仓库格式错误，应为：owner/repo";
            }
            String owner = repoParts[0];
            String repo = repoParts[1];
            String workflowId = parts.length > 1 ? parts[1] : null;
            try {
                List<Map<String, Object>> runs = gitHubClient.listWorkflowRuns(owner, repo, workflowId, 10);
                if (runs == null || runs.isEmpty()) {
                    return "❌ 未找到工作流运行记录";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("📋 最近的工作流运行\n\n");
                for (Map<String, Object> run : runs) {
                    int runId = (Integer) run.get("id");
                    String status = (String) run.get("status");
                    String conclusion = (String) run.get("conclusion");
                    String branch = (String) run.get("head_branch");
                    sb.append(String.format("• #%d - %s (分支: %s, 结果: %s)\n", 
                            runId, status, branch, 
                            conclusion != null ? conclusion : "进行中"));
                }
                return sb.toString();
            } catch (Exception e) {
                log.error("获取工作流运行列表失败", e);
                return "❌ 获取失败: " + e.getMessage();
            }
        }

        // /github cancel <owner/repo> <run-id>
        if (githubCmd.startsWith("cancel ")) {
            String[] parts = githubCmd.substring(7).split("\\s+", 2);
            if (parts.length < 2) {
                return "❌ 用法：/github cancel <owner/repo> <run-id>";
            }
            String[] repoParts = parts[0].split("/", 2);
            if (repoParts.length < 2) {
                return "❌ 仓库格式错误，应为：owner/repo";
            }
            String owner = repoParts[0];
            String repo = repoParts[1];
            try {
                int runId = Integer.parseInt(parts[1]);
                String result = gitHubClient.cancelWorkflowRun(owner, repo, runId);
                return String.format("""
                        🛑 取消工作流运行

                        📂 仓库：%s/%s
                        🆔 运行 ID：%d
                        🕐 时间：%s

                        %s
                        """, owner, repo, runId,
                           LocalDateTime.now(ZONE).format(FORMATTER),
                           result);
            } catch (NumberFormatException e) {
                return "❌ 运行 ID 必须是数字";
            }
        }

        return """
                ❌ GitHub 命令格式错误

                🚀 可用命令：
                • /github workflow <owner/repo> <工作流文件> <分支> - 触发工作流
                • /github status <owner/repo> <run-id> - 查询运行状态
                • /github list <owner/repo> [工作流文件] - 列出最近运行
                • /github cancel <owner/repo> <run-id> - 取消运行

                💡 示例：
                /github workflow Claire-bit-cpu/Test deploy-dev.yml develop
                /github status Claire-bit-cpu/Test 123456
                /github list Claire-bit-cpu/Test
                /github cancel Claire-bit-cpu/Test 123456
                """;
    }
}
