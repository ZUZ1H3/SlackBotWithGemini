package com.zuzihe.slackbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SlackWebClient {

    @Value("${slack.botToken}")
    private String botToken;

    private final WebClient slackClient = WebClient.create("https://slack.com/api");

    // 일반 메시지 전송
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
                .subscribe(
                        resp -> log.info("Slack 메시지 전송 완료: {}", resp),
                        error -> log.error("Slack 메시지 전송 실패: {}", error.getMessage(), error)
                );
    }

    // 스레드 메시지 전송
    public void sendMessageWithThread(String channelId, String message, String threadTs) {
        Map<String, Object> requestBody = Map.of(
                "channel", channelId,
                "text", message,
                "thread_ts", threadTs
        );
        slackClient.post()
                .uri("https://slack.com/api/chat.postMessage")
                .header("Authorization", "Bearer " + botToken)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .subscribe(
                response -> log.info("스레드 메시지 전송 완료: {}", response),
                error -> log.error("스레드 메시지 전송 실패", error)
        );
    }

    // 홈 탭 업데이트
    public void publishAppHome(String userId) {
        boolean isLinked = isAichatterLinked(userId); // 로그인 여부 판단

        Map<String, Object> view = Map.of(
                "type", "home",
                "blocks", isLinked ? getlinkedBlocks() : getUnlinkedBlocks()
        );

        Map<String, Object> payload = Map.of(
                "user_id", userId,
                "view", view
        );

        slackClient.post()
                .uri("/views.publish")
                .header("Authorization", "Bearer " + botToken)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        resp -> log.info("홈 탭 전송 성공: {}", resp),
                        err -> log.error("홈 탭 전송 실패: {}", err.getMessage(), err)
                );
    }
    // 로그인된 사용자용 홈 탭
    private List<Map<String, Object>> getlinkedBlocks() {
        return List.of(
                section("👋 *안녕하세요, aichatter입니다.*"),
                divider(),
                section("*나의 문서봇*"),
                divider(),
                sectionWithButton("*apispec-bot · aichatter*\n최근 대화한 날짜 · *1일 전*",
                        button("열기", "open_docbot_apispec")),
                sectionWithButton("*영업지원 문서봇*\n최근 대화한 날짜 · *2025-07-31 09:15*",
                        button("열기", "open_docbot_sales"))
        );
    }

    // 로그인 안 된 사용자용 홈 탭
    private List<Map<String, Object>> getUnlinkedBlocks() {
        return List.of(
                section("* aichatter를 슬랙에서 사용하려면 먼저 계정을 연동해주세요.*"),
                section("""
                        aichatter를 연동하면 다음 기능을 사용할 수 있어요.

                        • `/aichatter` 명령어로 바로 질문
                        • 문서봇 선택 후 대화형 질의
                        • 질문 기록 자동 저장
                        • 사내 데이터 기반 답변
                        """),
                divider(),
                Map.of("type", "actions", "elements", List.of(
                        button("🔗 aichatter 로그인하기", "go_to_login")
                ))
        );
    }


    // 로그인 링크 업데이트
    public void updateHomeViewWithLoginLink(String userId, String loginUrl) {
        log.info("로그인 링크로 홈탭 업데이트 - 사용자: {}, URL: {}", userId, loginUrl);

        Map<String, Object> view = Map.of(
                "type", "home",
                "blocks", List.of(
                        section("👉 <" + loginUrl + "|aichatter 로그인하기>"),
                        Map.of("type", "actions", "elements", List.of(
                                button("🔄 새로고침", "refresh_home_tab")
                        ))
                )
        );

        Map<String, Object> payload = Map.of(
                "user_id", userId,
                "view", view
        );

        slackClient.post()
                .uri("/views.publish")
                .header("Authorization", "Bearer " + botToken)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        resp -> log.info("로그인 링크 홈탭 업데이트 성공: {}", resp),
                        err -> log.error("로그인 링크 홈탭 업데이트 실패: {}", err.getMessage(), err)
                );
    }

    private boolean isAichatterLinked(String slackUserId) {
        // TODO: DB 조회 실제 로직으로 대체
        return false;
    }

    ///**************************헬퍼메서드
    public static Map<String, Object> section(String markdownText) {
        return Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text", markdownText)
        );
    }

    // 구분선(divider)
    public static Map<String, Object> divider() {
        return Map.of("type", "divider");
    }

    // 버튼
    public static Map<String, Object> button(String text, String actionId) {
        return Map.of(
                "type", "button",
                "text", Map.of("type", "plain_text", "text", text),
                "action_id", actionId
        );
    }

    public static Map<String, Object> sectionWithButton(String markdownText, Map<String, Object> button) {
        return Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text", markdownText),
                "accessory", button
        );
    }

}
