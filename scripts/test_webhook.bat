@echo off
echo ========= 测试 GitHub Webhook =========
echo.

REM 测试 Push 事件
curl -X POST http://localhost:8082/github-webhook ^
  -H "Content-Type: application/json" ^
  -H "X-GitHub-Event: push" ^
  -d "{\"repository\":{\"full_name\":\"test/repo\"},\"pusher\":{\"name\":\"testuser\"},\"head_commit\":{\"message\":\"Test commit\"}}"

echo.
echo ========= 测试完成 =========
pause
