# 部署配置完全指南

本文档提供完整的部署配置指南，包括多种通知方案。

## 📋 目录

1. [快速开始](#快速开始)
2. [方案 A：直接飞书 Webhook 通知](#方案-a直接飞书-webhook-通知)
3. [方案 B：部署回调通知](#方案-b部署回调通知)
4. [配置检查清单](#配置检查清单)
5. [故障排查](#故障排查)
6. [安全建议](#安全建议)

---

## 快速开始

### 1. 配置 GitHub Variables（非敏感配置）

进入 GitHub 仓库 → **Settings** → **Variables** → **Actions**，点击 **New repository variable** 添加以下变量：

#### 默认服务器（default）
| 变量名 | 描述 | 必填 | 示例 |
|--------|------|------|------|
| `DEPLOY_DEFAULT_HOST` | 服务器 SSH 主机地址 | 是 | `192.168.1.100` |
| `DEPLOY_DEFAULT_USER` | SSH 用户名 | 是 | `root` |
| `DEPLOY_DEFAULT_PATH` | 部署路径（可选，默认 `/app`） | 否 | `/var/www/app` |
| `DEPLOY_DEFAULT_HEALTH_URL` | 健康检查 URL（可选） | 否 | `http://localhost:3000/health` |

#### Server1/Server2
| 变量名 | 描述 | 必填 | 示例 |
|--------|------|------|------|
| `DEPLOY_SERVER1_HOST` | Server1 SSH 主机地址 | 是 | `server1.example.com` |
| `DEPLOY_SERVER1_USER` | Server1 SSH 用户名 | 是 | `deploy` |
| `DEPLOY_SERVER1_PATH` | Server1 部署路径（可选） | 否 | `/app` |
| `DEPLOY_SERVER1_HEALTH_URL` | Server1 健康检查 URL（可选） | 否 | - |
| `DEPLOY_SERVER2_HOST` | Server2 SSH 主机地址 | 是 | `server2.example.com` |
| `DEPLOY_SERVER2_USER` | Server2 SSH 用户名 | 是 | `deploy` |
| `DEPLOY_SERVER2_PATH` | Server2 部署路径（可选） | 否 | `/app` |
| `DEPLOY_SERVER2_HEALTH_URL` | Server2 健康检查 URL（可选） | 否 | - |

#### 阿里云（aliyun）
| 变量名 | 描述 | 必填 | 示例 |
|--------|------|------|------|
| `ALIYUN_REGION` | 区域（可选，默认 `cn-hangzhou`） | 否 | `cn-shanghai` |
| `ALIYUN_DEPLOY_TYPE` | 部署类型：`ecs` \| `function` \| `oss` | 否 | `ecs` |
| `ALIYUN_ECS_INSTANCE_ID` | ECS 实例 ID（ECS 部署时必填） | 条件 | `i-xxx` |
| `ALIYUN_ECS_HOST` | ECS SSH 主机地址（ECS 部署时必填） | 条件 | `ecs.example.com` |
| `ALIYUN_ECS_USER` | ECS SSH 用户名（可选，默认 `root`） | 否 | `root` |
| `ALIYUN_ECS_PATH` | ECS 部署路径（可选，默认 `/app`） | 否 | `/app` |
| `ALIYUN_OSS_BUCKET` | OSS Bucket 名称（OSS 部署时必填） | 条件 | `my-bucket` |
| `ALIYUN_OSS_ENDPOINT` | OSS Endpoint（OSS 部署时必填） | 条件 | `oss-cn-hangzhou.aliyuncs.com` |
| `ALIYUN_FC_SERVICE` | 函数计算服务名（函数计算部署时必填） | 条件 | `my-service` |
| `ALIYUN_FC_FUNCTION` | 函数计算函数名（函数计算部署时必填） | 条件 | `my-function` |
| `ALIYUN_HEALTH_URL` | 健康检查 URL（可选） | 否 | - |

#### AWS
| 变量名 | 描述 | 必填 | 示例 |
|--------|------|------|------|
| `AWS_REGION` | 区域（可选，默认 `us-east-1`） | 否 | `us-west-2` |
| `AWS_DEPLOY_TYPE` | 部署类型：`ec2` \| `lambda` \| `s3` | 否 | `ec2` |
| `AWS_EC2_INSTANCE_ID` | EC2 实例 ID（EC2 部署时必填） | 条件 | `i-xxx` |
| `AWS_EC2_HOST` | EC2 SSH 主机地址（EC2 部署时必填） | 条件 | `ec2.example.com` |
| `AWS_EC2_USER` | EC2 SSH 用户名（可选，默认 `ubuntu`） | 否 | `ubuntu` |
| `AWS_EC2_PATH` | EC2 部署路径（可选，默认 `/app`） | 否 | `/app` |
| `AWS_S3_BUCKET` | S3 Bucket 名称（S3 部署时必填） | 条件 | `my-bucket` |
| `AWS_S3_REGION` | S3 区域（可选） | 否 | `us-east-1` |
| `AWS_LAMBDA_FUNCTION_NAME` | Lambda 函数名（Lambda 部署时必填） | 条件 | `my-function` |
| `AWS_HEALTH_URL` | 健康检查 URL（可选） | 否 | - |

#### 腾讯云（tencent）
| 变量名 | 描述 | 必填 | 示例 |
|--------|------|------|------|
| `TENCENT_REGION` | 区域（可选，默认 `ap-guangzhou`） | 否 | `ap-shanghai` |
| `TENCENT_DEPLOY_TYPE` | 部署类型：`cvm` \| `scf` \| `cos` | 否 | `cvm` |
| `TENCENT_CVM_INSTANCE_ID` | CVM 实例 ID（CVM 部署时必填） | 条件 | `ins-xxx` |
| `TENCENT_CVM_HOST` | CVM SSH 主机地址（CVM 部署时必填） | 条件 | `cvm.example.com` |
| `TENCENT_CVM_USER` | CVM SSH 用户名（可选，默认 `root`） | 否 | `root` |
| `TENCENT_CVM_PATH` | CVM 部署路径（可选，默认 `/app`） | 否 | `/app` |
| `TENCENT_COS_BUCKET` | COS Bucket 名称（COS 部署时必填） | 条件 | `my-bucket` |
| `TENCENT_COS_REGION` | COS 区域（可选） | 否 | `ap-guangzhou` |
| `TENCENT_SCF_FUNCTION_NAME` | 云函数名称（SCF 部署时必填） | 条件 | `my-function` |
| `TENCENT_SCF_NAMESPACE` | 云函数命名空间（可选，默认 `default`） | 否 | `default` |
| `TENCENT_HEALTH_URL` | 健康检查 URL（可选） | 否 | - |

### 2. 配置 GitHub Secrets（敏感信息）

进入 GitHub 仓库 → **Settings** → **Secrets and variables** → **Actions** → **Secrets** 标签页，点击 **New repository secret** 添加以下密钥：

#### 默认服务器（default）
| 密钥名 | 描述 | 必填 |
|--------|------|------|
| `DEPLOY_DEFAULT_SSH_KEY` | 服务器 SSH 私钥 | 是 |

#### Server1/Server2
| 密钥名 | 描述 | 必填 |
|--------|------|------|
| `DEPLOY_SERVER1_SSH_KEY` | Server1 SSH 私钥 | 是 |
| `DEPLOY_SERVER2_SSH_KEY` | Server2 SSH 私钥 | 是 |

#### 阿里云（aliyun）
| 密钥名 | 描述 | 必填 |
|--------|------|------|
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AccessKey ID | 是 |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret | 是 |
| `ALIYUN_ECS_SSH_KEY` | ECS SSH 私钥（ECS 部署时必填） | 条件 |

#### AWS
| 密钥名 | 描述 | 必填 |
|--------|------|------|
| `AWS_ACCESS_KEY_ID` | AWS Access Key ID | 是 |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Access Key | 是 |
| `AWS_EC2_SSH_KEY` | EC2 SSH 私钥（EC2 部署时必填） | 条件 |

#### 腾讯云（tencent）
| 密钥名 | 描述 | 必填 |
|--------|------|------|
| `TENCENT_SECRET_ID` | 腾讯云 SecretId | 是 |
| `TENCENT_SECRET_KEY` | 腾讯云 SecretKey | 是 |
| `TENCENT_CVM_SSH_KEY` | CVM SSH 私钥（CVM 部署时必填） | 条件 |

#### 通知配置（方案 A 需要）
| 密钥名 | 描述 | 必填 |
|--------|------|------|
| `FEISHU_DEPLOY_WEBHOOK` | 飞书 Webhook 地址 | 否 |
| `DINGTALK_WEBHOOK` | 钉钉 Webhook 地址（可选） | 否 |
| `SLACK_WEBHOOK` | Slack Webhook 地址（可选） | 否 |

### 3. 触发部署

1. 进入 GitHub 仓库 → **Actions** 标签页
2. 选择 **Deploy to Dev Environment** 工作流
3. 点击 **Run workflow**
4. 选择：
   - **分支/Tag/SHA**：要部署的代码版本（默认 `develop`）
   - **部署目标**：选择部署目标（`default`/`server1`/`server2`/`aliyun`/`aws`/`tencent`）
5. 点击 **Run workflow** 确认

---

## 方案 A：直接飞书 Webhook 通知

### 概述

GitHub Actions 部署完成后**直接调用飞书 Webhook** 发送通知，无需回调。

### 配置步骤

#### 第1步：获取飞书群机器人 Webhook 地址

1. 打开飞书群 → 右上角 `...` → **群设置**
2. **群机器人** → **添加机器人** → **自定义机器人**
3. 设置机器人名称（如 `部署通知机器人`）
4. 复制 **Webhook 地址**，格式类似：
   ```
   https://open.feishu.cn/open-apis/bot/v2/hook/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   ```

#### 第2步：在 GitHub 仓库中配置 Secret

1. 打开你的 GitHub 仓库页面
2. **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. 填写：
   - **Name**: `FEISHU_DEPLOY_WEBHOOK`
   - **Secret**: 粘贴第1步复制的 Webhook 地址
6. 点击 **Add secret**

#### 第3步：触发部署

通过飞书机器人发送：
```
/deploy dev
```

### 工作流文件说明

| 文件 | 环境 | 触发方式 | 保护措施 |
|------|------|----------|----------|
| `deploy-dev.yml` | 开发环境 | 手动触发 | 无 |
| `deploy-test.yml` | 测试环境 | 手动触发 | 无 |
| `deploy-staging.yml` | 预发布环境 | 手动触发 | 无 |
| `deploy-prod.yml` | 生产环境 | 手动触发 | 需输入 `DEPLOY-PROD` 确认 + GitHub 环境保护 |

### 通知卡片效果

**成功通知**（绿色卡片）：
- 环境、仓库、分支、操作者、时间
- 查看 GitHub Actions 按钮

**失败通知**（红色卡片）：
- 同上，颜色为红色
- 提示立即处理

### 自定义通知内容

编辑 `.github/workflows/deploy-*.yml` 中的 `📢 发送飞书通知` 步骤，修改 `card` 内容即可。

### 常见问题

#### Q: 通知没有收到？

- 检查 `FEISHU_DEPLOY_WEBHOOK` secret 是否配置正确
- 检查工作流日志中是否有 curl 错误
- 飞书 Webhook 有频率限制（每分钟 20 条）

#### Q: 想要 @ 特定人员？

在飞书 Webhook 的 `content` 中添加 `<at user_id="ou_xxx">@某人</at>`

---

## 方案 B：部署回调通知

### 概述

GitHub Actions 部署完成后，通过回调 Java 应用来发送飞书通知，实现真正的部署成功检测。

### 工作原理

```
用户执行 /deploy dev
    ↓
系统生成 deployId
    ↓
触发 GitHub Actions 工作流（传递 deployId 和 callbackUrl）
    ↓
GitHub Actions 执行部署
    ↓
部署完成（成功/失败）
    ↓
GitHub Actions 调用 callbackUrl 通知结果
    ↓
系统收到回调，发送飞书通知
    ↓
✅ 用户收到真正的部署成功/失败通知
```

### 配置步骤

#### 步骤 1：配置 Java 应用

在 `application.yaml` 或环境变量中配置以下参数：

```yaml
github:
  callback:
    # 回调 URL（Java 应用对公网暴露的地址）
    # GitHub Actions 部署完成后会调用此地址通知部署结果
    url: ${GITHUB_CALLBACK_URL:http://your-public-domain.com:8082/github/callback}
    
    # 回调密钥（可选，用于验证回调请求）
    # 如果配置，GitHub Actions 会使用 HMAC-SHA256 签名回调请求
    secret: ${GITHUB_CALLBACK_SECRET:}
```

或者通过环境变量配置：

```bash
# Windows (cmd)
set GITHUB_CALLBACK_URL=http://your-public-domain.com:8082/github/callback
set GITHUB_CALLBACK_SECRET=your-secret-key

# Linux/Mac
export GITHUB_CALLBACK_URL=http://your-public-domain.com:8082/github/callback
export GITHUB_CALLBACK_SECRET=your-secret-key
```

#### 步骤 2：确保 Java 应用有公网可访问的地址

GitHub Actions 在云端运行，需要能访问你的 Java 应用。有以下几种方案：

**方案 A：云服务器部署**

如果你的 Java 应用部署在云服务器（如阿里云、腾讯云、AWS 等），直接使用服务器的公网地址：

```yaml
github:
  callback:
    url: http://your-server.com:8082/github/callback
```

**方案 B：本地开发测试（内网穿透）**

如果你在本地开发测试，需要使用内网穿透工具暴露本地服务。

**使用 ngrok**：

1. 下载并安装 [ngrok](https://ngrok.com/)
2. 启动穿透：
   ```bash
   ngrok http 8082
   ```
3. 终端会显示一个公网地址，类似：
   ```
   Forwarding  https://xxxx-xx-xx-xx-xx.ngrok-free.app -> http://localhost:8082
   ```
4. 使用这个地址配置：
   ```yaml
   github:
     callback:
       url: https://xxxx-xx-xx-xx-xx.ngrok-free.app/github/callback
   ```

**使用 frp**（自建穿透）：

如果你有自己的云服务器，可以用 frp 做内网穿透，获得更稳定的地址。

**方案 C：GitHub Actions Self-hosted Runner**

如果你使用自托管的 GitHub Actions Runner，且 Runner 和 Java 应用在同一内网，可以直接使用内网地址：

```yaml
github:
  callback:
    url: http://内网IP:8082/github/callback
```

#### 步骤 3：配置飞书通知

确保 `NotificationService` 已正确配置，能够发送飞书通知。

在 `application.yaml` 中配置默认通知群聊 ID：

```yaml
notification:
  default-chat-ids: oc_xxxxxxxxxxxx  # 飞书群聊 ID
```

或者通过环境变量配置：

```bash
export NOTIFICATION_DEFAULT_CHAT_IDS=oc_xxxxxxxxxxxx
```

#### 步骤 4：测试部署回调

1. 启动 Java 应用
2. 在飞书群中发送 `/deploy dev`
3. 确认部署
4. 等待 GitHub Actions 完成部署
5. 检查是否收到部署结果通知

### 回调流程

```
用户输入 /deploy dev
    ↓
DeployCommandHandler 处理指令
    ↓
生成 deployId 和 callbackToken
    ↓
存储 callbackToken 到 Redis（key: deploy:callback:token:{deployId}）
    ↓
构建 callbackUrl = {baseUrl}?deployId={deployId}&token={callbackToken}
    ↓
触发 GitHub Actions 工作流（传入 callback_url input）
    ↓
GitHub Actions 执行部署
    ↓
部署完成，调用 callbackUrl 发送回调请求
    ↓
GitHubCallbackController 接收回调
    ↓
GitHubCallbackService 验证 token（从 Redis 读取并比对）
    ↓
验证成功，删除 Redis 中的 token（一次性使用）
    ↓
发送飞书通知（通过 NotificationService）
```

### 安全考虑

1. **Token 验证**：
   - 每个部署请求都会生成唯一的 `deployId` 和 `callbackToken`
   - Token 存储在 Redis 中，24 小时过期
   - 回调时验证 token，验证成功后立即删除（一次性使用）
   - 防止恶意第三方伪造回调请求

2. **签名验证**（可选）：
   - 如果配置了 `github.callback.secret`，GitHub Actions 会使用 HMAC-SHA256 对回调请求进行签名
   - Java 应用会验证签名，确保回调请求来自 GitHub Actions

3. **HTTPS**：
   - 建议使用 HTTPS 保护回调 URL，防止 token 泄露
   - 如果使用 ngrok，默认提供 HTTPS 端点

### 故障排查

#### 问题 1：GitHub Actions 无法调用回调 URL

**可能原因**：
- Java 应用没有公网可访问的地址
- 防火墙阻止了入站连接
- 回调 URL 配置错误

**解决方法**：
- 检查 `github.callback.url` 配置是否正确
- 使用 `curl` 测试回调 URL 是否可访问
- 检查服务器防火墙规则

#### 问题 2：回调 token 验证失败

**可能原因**：
- Redis 不可用
- Token 已过期（24 小时）
- Token 已被使用（一次性）
- URL 中的 token 参数不正确

**解决方法**：
- 检查 Redis 是否正常运行
- 检查日志中的 `deployId` 和 `token` 是否正确
- 重新触发部署，生成新的 token

#### 问题 3：飞书通知未发送

**可能原因**：
- `NotificationService` 未注入
- 默认群聊 ID 未配置
- 飞书 API 调用失败

**解决方法**：
- 检查 `notification.default-chat-ids` 配置是否正确
- 检查 `FeishuClient` 配置是否正确（app-id、app-secret）
- 查看日志中的错误信息

### 示例配置

#### 本地开发（使用 ngrok）

1. 启动 ngrok：
   ```bash
   ngrok http 8082
   ```

2. 配置环境变量：
   ```bash
   set GITHUB_CALLBACK_URL=https://xxxx-xx-xx-xx-xx.ngrok-free.app/github/callback
   set GITHUB_CALLBACK_SECRET=your-secret-key
   set NOTIFICATION_DEFAULT_CHAT_IDS=oc_xxxxxxxxxxxx
   ```

3. 启动 Java 应用

#### 云服务器部署

1. 配置 `application.yaml`：
   ```yaml
   github:
     callback:
       url: http://your-server.com:8082/github/callback
       secret: your-secret-key
   
   notification:
     default-chat-ids: oc_xxxxxxxxxxxx
   ```

2. 启动 Java 应用

---

## 配置检查清单

### 🔐 Secrets（敏感信息）

#### 1️⃣ 通用配置

| Secret 名称 | 说明 | 必需 | 示例 |
|-------------|------|------|------|
| `FEISHU_DEPLOY_WEBHOOK` | 飞书机器人 Webhook 地址（方案 A） | ✅ 是 | `https://open.feishu.cn/open-apis/bot/v2/hook/xxx` |
| `GITHUB_TOKEN` | GitHub Personal Access Token | ✅ 是 | `ghp_xxxxxxxxxxxx` |

#### 2️⃣ 默认目标（default）

| Secret 名称 | 说明 | 必需 |
|-------------|------|------|
| `DEPLOY_DEFAULT_SSH_KEY` | 默认服务器 SSH 私钥 | ❌ 可选（未配置则使用模拟部署） |

#### 3️⃣ 服务器 1（server1）

| Secret 名称 | 说明 | 必需 |
|-------------|------|------|
| `DEPLOY_SERVER1_SSH_KEY` | 服务器 1 SSH 私钥 | ❌ 可选 |

#### 4️⃣ 服务器 2（server2）

| Secret 名称 | 说明 | 必需 |
|-------------|------|------|
| `DEPLOY_SERVER2_SSH_KEY` | 服务器 2 SSH 私钥 | ❌ 可选 |

#### 5️⃣ 阿里云（aliyun）

| Secret 名称 | 说明 | 必需 |
|-------------|------|------|
| `ALIYUN_ECS_SSH_KEY` | 阿里云 ECS SSH 私钥 | ❌ 可选 |
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AccessKey ID（用于 OSS/函数计算） | ❌ 可选 |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret | ❌ 可选 |

#### 6️⃣ AWS（aws）

| Secret 名称 | 说明 | 必需 |
|-------------|------|------|
| `AWS_EC2_SSH_KEY` | AWS EC2 SSH 私钥 | ❌ 可选 |
| `AWS_ACCESS_KEY_ID` | AWS Access Key ID | ❌ 可选 |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Access Key | ❌ 可选 |

#### 7️⃣ 腾讯云（tencent）

| Secret 名称 | 说明 | 必需 |
|-------------|------|------|
| `TENCENT_CVM_SSH_KEY` | 腾讯云 CVM SSH 私钥 | ❌ 可选 |
| `TENCENT_SECRET_ID` | 腾讯云 SecretId | ❌ 可选 |
| `TENCENT_SECRET_KEY` | 腾讯云 SecretKey | ❌ 可选 |

### 📝 Variables（非敏感配置）

#### 1️⃣ 默认目标（default）

| Variable 名称 | 说明 | 默认值 | 必需 |
|---------------|------|--------|------|
| `DEPLOY_DEFAULT_HOST` | 服务器公网 IP 或域名 | - | ❌ 可选 |
| `DEPLOY_DEFAULT_USER` | SSH 用户名 | `root` | ❌ 可选 |
| `DEPLOY_DEFAULT_PATH` | 部署路径 | `/app` | ❌ 可选 |
| `DEPLOY_DEFAULT_HEALTH_URL` | 健康检查 URL | - | ❌ 可选 |

#### 2️⃣ 服务器 1（server1）

| Variable 名称 | 说明 | 默认值 | 必需 |
|---------------|------|--------|------|
| `DEPLOY_SERVER1_HOST` | 服务器 1 公网 IP 或域名 | - | ❌ 可选 |
| `DEPLOY_SERVER1_USER` | SSH 用户名 | `root` | ❌ 可选 |
| `DEPLOY_SERVER1_PATH` | 部署路径 | `/app` | ❌ 可选 |
| `DEPLOY_SERVER1_HEALTH_URL` | 健康检查 URL | - | ❌ 可选 |

#### 3️⃣ 服务器 2（server2）

| Variable 名称 | 说明 | 默认值 | 必需 |
|---------------|------|--------|------|
| `DEPLOY_SERVER2_HOST` | 服务器 2 公网 IP 或域名 | - | ❌ 可选 |
| `DEPLOY_SERVER2_USER` | SSH 用户名 | `root` | ❌ 可选 |
| `DEPLOY_SERVER2_PATH` | 部署路径 | `/app` | ❌ 可选 |
| `DEPLOY_SERVER2_HEALTH_URL` | 健康检查 URL | - | ❌ 可选 |

---

## 故障排查

### 部署失败：SSH 连接失败

- 检查 Variables 中配置的主机地址和用户名是否正确
- 检查 Secrets 中配置的 SSH 私钥是否正确
- 确保服务器的 SSH 公钥已添加到 `~/.ssh/authorized_keys`

### 部署失败：权限不足

- 检查 SSH 用户是否有部署路径的读写权限
- 检查是否有权限执行部署命令（如 `systemctl restart`）

### 通知未发送

- 检查 Secrets 中配置的 Webhook 地址是否正确
- 检查 Webhook 是否已在飞书/钉钉/Slack 中正确配置

### 提示"Permission denied (publickey)"

**原因**：SSH 密钥未正确配置

**解决**：
- 检查 Secret 中的私钥格式是否完整
- 检查服务器上是否已添加公钥到 `~/.ssh/authorized_keys`
- 检查私钥权限是否为 600

### 提示"Host key verification failed"

**原因**：首次连接服务器，未信任服务器指纹

**解决**：
- 工作流中已添加 `-o StrictHostKeyChecking=no` 参数
- 如果仍然报错，手动连接一次服务器以信任指纹

---

## 安全建议

1. **不要将敏感信息提交到代码仓库**：所有敏感信息（SSH 私钥、API 密钥等）都应存储在 GitHub Secrets 中
2. **定期轮换密钥**：定期更换 SSH 密钥和云服务商 API 密钥
3. **最小权限原则**：为部署用户和 API 密钥分配最小必要权限
4. **审计日志**：定期检查 GitHub Actions 执行日志和云服务商的操作日志

---

## 自定义部署目标

如果需要添加新的部署目标，请按以下步骤操作：

### 1. 更新配置文件

编辑 `.github/deploy-config.yml`，添加新的部署目标配置。

### 2. 更新工作流文件

编辑 `.github/workflows/deploy-dev.yml`，在 `workflow_dispatch` 输入的 `target` 选项中添加新的目标名称。

### 3. 配置对应的 Variables 和 Secrets

按照命名规范添加新的 GitHub Variables 和 Secrets。

---

## 相关文件

- `.github/deploy-config.yml` - 部署配置文件
- `.github/workflows/deploy-dev.yml` - 部署工作流文件
- `.github/workflows/deploy-prod.yml` - 生产环境部署工作流（如有）
- `src/main/java/com/example/IntelligentRobot/controller/DeployCallbackController.java` - 回调接口（方案 B）
- `src/main/java/com/example/IntelligentRobot/service/GitHubCallbackService.java` - 回调服务（方案 B）

---

**最后更新**：2026-06-08
