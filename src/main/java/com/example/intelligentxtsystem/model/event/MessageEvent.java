package com.example.intelligentxtsystem.model.event;

/**
 * 飞书消息事件业务对象
 */
//这是DTO，data transfer object，DTO 就像是一个专门用来在不同系统或模块之间传递数据的“快递盒”或“数据容器”。它通常只包含一系列属性（字段）以及对应的获取和设置方法（Getter/Setter），而不包含任何复杂的业务逻辑。是一中软件设计模式
    //因为是业务数据，是飞书消息事件的业务对象，所以放在model（用来放数据）中
public record MessageEvent(
        String text,
        String chatId
) {
}