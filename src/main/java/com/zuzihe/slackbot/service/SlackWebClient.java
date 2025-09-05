package com.zuzihe.slackbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
public class SlackWebClient {

    @Value("${slack.botToken}")
    private String botToken;

    private final WebClient slackClient = WebClient.create("https://slack.com/api");

    public void sendMessage(String channel, String text) {
        Map<String, Object> payload = Map.of(
                "channel", channel,
                "text", text
        );

        slackClient.post()
                .uri("/chat.postMessage")
                .header("Authorization", "Bearer " + botToken)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(resp -> {
                    System.out.println("Slack 메시지 전송 완료: " + resp);
                }, error -> {
                    System.err.println("Slack 메시지 전송 실패: " + error.getMessage());
                });
    }
}
