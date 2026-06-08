@echo off
echo ========== 环境变量检查 ==========
echo.

echo [1] GITHUB_TOKEN:
if defined GITHUB_TOKEN (echo   已配置) else (echo   ❌ 未配置)

echo [2] GITHUB_WEBHOOK_SECRET:
if defined GITHUB_WEBHOOK_SECRET (echo   已配置) else (echo   ❌ 未配置)

echo [3] NOTIFICATION_DEFAULT_CHAT_IDS:
if defined NOTIFICATION_DEFAULT_CHAT_IDS (echo   ✅ 已配置: %NOTIFICATION_DEFAULT_CHAT_IDS%) else (echo   ❌ 未配置)

echo [4] QIANWEN_API_KEY:
if defined QIANWEN_API_KEY (echo   已配置) else (echo   ❌ 未配置)

echo.
echo ========== 建议 ==========
echo 1. 确保所有环境变量已配置
echo 2. 重启应用程序使配置生效
echo 3. 检查 GitHub Webhook 配置
echo 4. 查看应用程序日志: type boot.log | findstr /i "GitHub|webhook"
echo.
pause
