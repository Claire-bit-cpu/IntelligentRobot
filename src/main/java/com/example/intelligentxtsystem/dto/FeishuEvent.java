package com.example.intelligentxtsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuEvent {
    private FeishuSender sender;
    private FeishuMessage message;
}