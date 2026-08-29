package com.qbank.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容接口客户端(默认适配 DeepSeek), 零第三方依赖
 */
@Component
public class AiHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AiHttpClient.class);

    private final Environment env;
    private final ObjectMapper objectMapper;

    public AiHttpClient(Environment env, ObjectMapper objectMapper) {
        this.env = env;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        String enabled = env.getProperty("qbank.ai.enabled", "false");
        return "true".equalsIgnoreCase(enabled) && StringUtils.hasText(env.getProperty("qbank.ai.api-key", ""));
    }

    public String chat(String prompt) throws IOException {
        String baseUrl = env.getProperty("qbank.ai.base-url", "https://api.deepseek.com");
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String apiKey = env.getProperty("qbank.ai.api-key", "");
        String model = env.getProperty("qbank.ai.model", "deepseek-chat");
        int timeout = Integer.parseInt(env.getProperty("qbank.ai.timeout-ms", "60000"));

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        String payload = objectMapper.writeValueAsString(body);

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        String text = readStream(status >= 400 ? conn.getErrorStream() : conn.getInputStream());
        if (status >= 400) {
            log.warn("AI request failed, status={}, body={}", status, text);
            throw new IOException("AI接口返回 " + status);
        }
        JsonNode root = objectMapper.readTree(text);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || !StringUtils.hasText(content.asText())) {
            throw new IOException("AI接口返回内容为空");
        }
        return content.asText();
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
        }
        is.close();
        return sb.toString();
    }
}
