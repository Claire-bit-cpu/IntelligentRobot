package com.example.IntelligentRobot.config;

import com.example.IntelligentRobot.task.TaskContext;
import com.example.IntelligentRobot.task.TaskStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * TaskContext 配置类
 * 用于在应用启动时注入 TaskStatusService
 */
@Configuration
public class TaskContextConfig {

    private static final Logger log = LoggerFactory.getLogger(TaskContextConfig.class);

    private final TaskStatusService taskStatusService;

    public TaskContextConfig(TaskStatusService taskStatusService) {
        this.taskStatusService = taskStatusService;
    }

    @PostConstruct
    public void init() {
        // 注入 TaskStatusService 到 TaskContext
        TaskContext.setTaskStatusService(taskStatusService);
        log.info("TaskStatusService 已注入到 TaskContext");
    }
}
