# 部署配置指南

本文档说明如何配置 GitHub Actions 部署工作流，实现灵活的多目标部署。

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

#### 通知配置
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

## 自定义部署目标

如果需要添加新的部署目标，请按以下步骤操作：

### 1. 更新配置文件

编辑 `.github/deploy-config.yml`，添加新的部署目标配置。

### 2. 更新工作流文件

编辑 `.github/workflows/deploy-dev.yml`，在 `workflow_dispatch` 输入的 `target` 选项中添加新的目标名称。

### 3. 配置对应的 Variables 和 Secrets

按照命名规范添加新的 GitHub Variables 和 Secrets。

## 配置示例

### 示例 1：配置默认服务器部署

**Variables：**
- `DEPLOY_DEFAULT_HOST`: `192.168.1.100`
- `DEPLOY_DEFAULT_USER`: `root`
- `DEPLOY_DEFAULT_PATH`: `/var/www/myapp`

**Secrets：**
- `DEPLOY_DEFAULT_SSH_KEY`: （你的 SSH 私钥内容）

### 示例 2：配置阿里云 ECS 部署

**Variables：**
- `ALIYUN_REGION`: `cn-hangzhou`
- `ALIYUN_DEPLOY_TYPE`: `ecs`
- `ALIYUN_ECS_INSTANCE_ID`: `i-xxxxxxxxxxxxxxxxx`
- `ALIYUN_ECS_HOST`: `your-ecs-host.example.com`
- `ALIYUN_ECS_USER`: `root`
- `ALIYUN_ECS_PATH`: `/app`

**Secrets：**
- `ALIYUN_ACCESS_KEY_ID`: （你的 AccessKey ID）
- `ALIYUN_ACCESS_KEY_SECRET`: （你的 AccessKey Secret）
- `ALIYUN_ECS_SSH_KEY`: （你的 SSH 私钥内容）

### 示例 3：配置 AWS EC2 部署

**Variables：**
- `AWS_REGION`: `us-east-1`
- `AWS_DEPLOY_TYPE`: `ec2`
- `AWS_EC2_INSTANCE_ID`: `i-xxxxxxxxxxxxxxxxx`
- `AWS_EC2_HOST`: `ec2-xx-xx-xx-xx.compute-1.amazonaws.com`
- `AWS_EC2_USER`: `ubuntu`
- `AWS_EC2_PATH`: `/app`

**Secrets：**
- `AWS_ACCESS_KEY_ID`: （你的 Access Key ID）
- `AWS_SECRET_ACCESS_KEY`: （你的 Secret Access Key）
- `AWS_EC2_SSH_KEY`: （你的 SSH 私钥内容）

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

## 安全建议

1. **不要将敏感信息提交到代码仓库**：所有敏感信息（SSH 私钥、API 密钥等）都应存储在 GitHub Secrets 中
2. **定期轮换密钥**：定期更换 SSH 密钥和云服务商 API 密钥
3. **最小权限原则**：为部署用户和 API 密钥分配最小必要权限
4. **审计日志**：定期检查 GitHub Actions 执行日志和云服务商的操作日志

## 相关文件

- `.github/deploy-config.yml` - 部署配置文件
- `.github/workflows/deploy-dev.yml` - 部署工作流文件
- `.github/workflows/deploy-prod.yml` - 生产环境部署工作流（如有）
