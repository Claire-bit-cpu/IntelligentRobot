package com.example.feishu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class FeishuMessageService {

    @Autowired
    private FeishuProperties feishu;

    @Autowired
    private RestTemplate restTemplate;

    public void sendTextToGroup(String text) {
        String token = getTenantAccessToken();

        String body = """
        {
          "receive_id": "%s",
          "msg_type": "text",
          "content": "{\\"text\\": \\"%s\\"}"
        }
        """.formatted(feishu.getChatId(), text);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity(
                "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id",
                new HttpEntity<>(body, headers),
                String.class
        );
    }

    private String getTenantAccessToken() {
        String body = """
        {
          "app_id": "%s",
          "app_secret": "%s"
        }
        """.formatted(feishu.getAppId(), feishu.getAppSecret());

        Map<?,?> resp = restTemplate.postForObject(
                "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/",
                new HttpEntity<>(body, jsonHeader()),
                Map.class
        );

        return (String) resp.get("tenant_access_token");
    }

    private HttpHeaders jsonHeader() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
