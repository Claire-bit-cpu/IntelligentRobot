/*
只要 Run Spring Boot
群就会立刻收到一条消息
 */

package com.example.feishu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SendTestMessageOnStartup implements CommandLineRunner {

    @Autowired
    private FeishuMessageService feishuMessageService;

    @Override
    public void run(String... args) {
        // 启动后自动执行
        feishuMessageService.sendTextToGroup("✅ Spring Boot 启动成功，飞书机器人已上线！");
    }
}