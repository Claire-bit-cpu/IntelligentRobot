@echo off
chcp 65001 > nul
echo ===== 消息合并功能演示脚本 =====
echo.

REM 配置部分（请修改为你的实际值）
set CHAT_ID=oc_c2132f28f9ae3f293ee11e3c06cf8c1d
set BASE_URL=http://localhost:8082

echo [信息] 使用配置：
echo    群聊 ID: %CHAT_ID%
echo    服务地址: %BASE_URL%
echo.

REM 检查是否配置了群聊ID
if "%CHAT_ID%"=="oc_你的群聊ID" (
    echo [错误] 请先修改此脚本，设置正确的群聊 ID
    pause
    exit /b 1
)

echo ===== 步骤1：测试消息去重功能 =====
echo [1/7] 发送第1条测试消息...
curl -X GET "%BASE_URL%/test/notify/demo/dedup-test?chatId=%CHAT_ID%"
echo.
echo [提示] 查看返回结果，应该显示"第一次发送：成功"
echo.
pause

echo.
echo [2/7] 立即发送相同消息（测试去重）...
curl -X GET "%BASE_URL%/test/notify/demo/dedup-test?chatId=%CHAT_ID%"
echo.
echo [提示] 查看返回结果，应该显示"第二次发送：已去重（正常）"
echo.
pause

echo.
echo ===== 步骤2：测试消息合并功能 =====
echo [3/7] 发送第1条构建失败通知...
curl -X GET "%BASE_URL%/test/notify/demo/build-failed?index=1&chatId=%CHAT_ID%"
timeout /t 2 /nobreak > nul

echo [4/7] 发送第2条构建失败通知...
curl -X GET "%BASE_URL%/test/notify/demo/build-failed?index=2&chatId=%CHAT_ID%"
timeout /t 2 /nobreak > nul

echo [5/7] 发送第3条构建失败通知（达到阈值，立即推送合并摘要）...
curl -X GET "%BASE_URL%/test/notify/demo/build-failed?index=3&chatId=%CHAT_ID%"
echo.
echo [提示] 查看飞书群聊，应该收到1条合并摘要消息
echo.
pause

echo.
echo ===== 步骤3：批量发送测试 =====
echo [6/7] 批量发送5条构建通知...
curl -X GET "%BASE_URL%/test/notify/demo/batch-test?chatId=%CHAT_ID%"
echo.
echo [提示] 查看飞书群聊，应该只收到1-2条合并摘要（而不是5条独立消息）
echo.
pause

echo.
echo ===== 步骤4：不同类型消息测试 =====
echo [7/7] 发送3种不同类型的通知...
curl -X GET "%BASE_URL%/test/notify/demo/mixed-types?chatId=%CHAT_ID%"
echo.
echo [提示] 查看飞书群聊，应该收到3条独立消息（不同类型不会合并）
echo.
pause

echo.
echo ===== 演示完成！=====
echo.
echo 总结：
echo    相同类型的消息会被合并（如3条构建通知 → 1条合并摘要）
echo    不同类型的消息不会合并（如构建、部署、告警各自独立）
echo    相同内容在短时间内只会推送一次（去重功能）
echo.
pause
