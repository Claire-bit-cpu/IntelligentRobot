package com.example.intelligentxtsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuSender {
    private SenderId sender_id;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SenderId {
        private String open_id;
        private String user_id;
        private String union_id;
        private String id_type;
    }
    
    /**
     * 获取发送者的 open_id
     * 优先返回 open_id，如果为空则返回 user_id
     */
    public String getOpenId() {
        if (sender_id != null) {
            if (sender_id.getOpen_id() != null) {
                return sender_id.getOpen_id();
            }
            if (sender_id.getUser_id() != null) {
                return sender_id.getUser_id();
            }
        }
        return null;
    }

    /**
     * 获取发送者的 user_id
     */
    public String getUserId() {
        if (sender_id != null && sender_id.getUser_id() != null) {
            return sender_id.getUser_id();
        }
        return null;
    }

    /**
     * 获取发送者 ID（统一入口）
     * 优先使用 user_id，如果为空则使用 open_id
     */
    public String getId() {
        if (sender_id != null) {
            if (sender_id.getUser_id() != null) {
                return sender_id.getUser_id();
            }
            if (sender_id.getOpen_id() != null) {
                return sender_id.getOpen_id();
            }
        }
        return null;
    }
}
