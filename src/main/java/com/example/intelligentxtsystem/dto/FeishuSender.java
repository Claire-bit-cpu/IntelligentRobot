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
}
