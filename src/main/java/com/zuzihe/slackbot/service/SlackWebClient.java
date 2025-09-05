package com.zuzihe.slackbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
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

    public void publishAppHome(String userId) {
        Map<String, Object> payload = Map.of(
                "user_id", userId,
                "view", Map.of(
                        "type", "home",
                        "blocks", List.of(
                                Map.of(
                                        "type", "section",
                                        "text", Map.of(
                                                "type", "mrkdwn",
                                                "text", "*aichatter에 오신 걸 환영합니다!* 🤖\n\n아래 기능을 사용해보세요:"
                                        )
                                ),
                                Map.of("type", "divider"),
                                Map.of(
                                        "type", "section",
                                        "text", Map.of(
                                                "type", "mrkdwn",
                                                "text", "• `/aichatter` 로 바로 질문하기\n• `@aichatter` 멘션으로 대화하기\n• 문서 요약/검색 기능도 곧 제공됩니다!"
                                        )
                                ),
                                Map.of(
                                        "type", "context",
                                        "elements", List.of(
                                                Map.of(
                                                        "type", "mrkdwn",
                                                        "text", "_이 홈탭은 자동으로 업데이트됩니다._"
                                                )
                                        )
                                )
                        )
                )
        );

        slackClient.post()
                .uri("/views.publish")
                .header("Authorization", "Bearer " + botToken)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(resp -> System.out.println("홈 탭 전송 성공: " + resp),
                        err -> System.err.println("홈 탭 전송 실패: " + err.getMessage()));
    }

}
