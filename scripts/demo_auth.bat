@echo off
chcp 65001 >nul
echo ================================
echo 权限管理演示脚本
echo ================================
echo.

echo 1. 查看当前权限名单
curl -s http://localhost:8082/api/auth/users | jq .
echo.

echo 按任意键继续...
pause >nul

echo.
echo 2. 添加新用户到开发者组
set /p OPEN_ID="请输入要添加的 Open ID (例如: ou_xxx): "
curl -s -X POST http://localhost:8082/api/auth/users/add ^
  -H "Content-Type: application/json" ^
  -d "{\"open_id\": \"%OPEN_ID%\", \"level\": \"developer\"}" | jq .
echo.

echo 按任意键继续...
pause >nul

echo.
echo 3. 验证新用户权限
curl -s -X POST http://localhost:8082/api/auth/users/check ^
  -H "Content-Type: application/json" ^
  -d "{\"open_id\": \"%OPEN_ID%\"}" | jq .
echo.

echo 按任意键继续...
pause >nul

echo.
echo 4. 移除用户权限
set /p REMOVE_ID="请输入要移除的 Open ID: "
curl -s -X DELETE http://localhost:8082/api/auth/users/%REMOVE_ID% | jq .
echo.

echo ================================
echo 演示完成！
echo ================================
pause
