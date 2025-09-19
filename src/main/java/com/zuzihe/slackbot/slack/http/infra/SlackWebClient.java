package com.zuzihe.slackbot.slack.http.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

import static com.zuzihe.slackbot.slack.http.util.SlackBlockBuilder.section;
import static com.zuzihe.slackbot.slack.http.util.SlackBlockBuilder.button;
import static com.zuzihe.slackbot.slack.http.util.SlackBlockBuilder.divider;
import static com.zuzihe.slackbot.slack.http.util.SlackBlockBuilder.sectionWithButton;
import static com.zuzihe.slackbot.slack.http.util.SlackBlockBuilder.urlButton;


@Slf4j
@Component
public class SlackWebClient {

    @Value("${slack.botToken}")
    private String botToken;

    private final WebClient slackClient = WebClient.create("https://slack.com/api");

    // 일반 메시지 전송
    public void sendMessage(String channel, String text) {
        Map<String, Object> payload = Map.of("channel", channel, "text", text);
        postToSlack("/chat.postMessage", payload, "메시지 전송 성공", "메시지 전송 실패");
    }

    // 스레드 메시지 전송
    public void sendMessageWithThread(String channelId, String message, String threadTs) {
        Map<String, Object> block = section(message);
        Map<String, Object> payload = Map.of(
                "channel", channelId,
                "thread_ts", threadTs,
                "blocks", List.of(block)
        );
        postToSlack("/chat.postMessage", payload, "스레드 메시지 전송 성공", "스레드 메시지 전송 실패");
    }

    // 홈 탭 업데이트
    public void publishAppHome(String userId) {
        Map<String, Object> view = Map.of(
                "type", "home",
                "blocks", isAichatterLinked(userId) ? getLinkedBlocks() : getUnlinkedBlocks(userId)
        );
        Map<String, Object> payload = Map.of("user_id", userId, "view", view);
        postToSlack("/views.publish", payload, "홈탭 전송 성공", "홈탭 전송 실패");
    }

    // 로그인된 사용자용 홈 탭
    private List<Map<String, Object>> getLinkedBlocks() {
        return List.of(
                section("👋 *안녕하세요, aichatter입니다.*\n"),
                divider(),
                section("*나의 문서봇*"),
                divider(),
                sectionWithButton("*아이채터 정보봇*\n최근 대화한 날짜 · *1일 전*",
                        button("채팅", "open_docbot_apispec")),
                sectionWithButton("*영업지원 문서봇*\n최근 대화한 날짜 · *2025-08-31 09:15*",
                        button("채팅", "open_docbot_sales"))
        );
    }

    // 로그인 안 된 사용자용 홈 탭
    private List<Map<String, Object>> getUnlinkedBlocks(String userId) {
        String loginUrl = "http://localhost:8081/slack/sign-in?slack_user_id=" + userId + "&team_id=" + "d1234";
        return List.of(
                section("* aichatter를 슬랙에서 사용하려면 먼저 계정을 연동해주세요.*"),
                divider(),
                Map.of("type", "actions", "elements", List.of(
                        urlButton("🔗 aichatter 로그인하기", loginUrl)
                ))
        );
    }

    public void sendWelcomeMessageWithButtons(String channelId, String threadTs) {
        List<Map<String, Object>> blocks = List.of(
                section("안녕하세요! \n저는 aichatter입니다."),
                section("\n아래는 예시 프롬프트입니다."),

                Map.of("type", "actions", "elements", List.of(
                        button("aichatter에 대해 알려주세요!!!", "latest_trends"),
                        button("문서봇을 만드는 방법이 무엇인강요?", "b2b_social_media")
                )),

                section("문서봇 이용하고싶ㅇ므면 아래에서 문서봇을 선택하세요."),

                Map.of("type", "actions", "elements", List.of(
                        button("지혜의 문서봇", "customer_feedback"),
                        button("아이채터 정보봇ㅎ", "product_brainstorm")
                ))
        );

        Map<String, Object> payload = Map.of(
                "channel", channelId,
                "thread_ts", threadTs,
                "text", "환영 메시지",
                "blocks", blocks
        );

        postToSlack("/chat.postMessage", payload, "환영 메시지 전송 성공", "환영 메시지 전송 실패");
    }

    // 스레드 제목 설정
    public void setThreadTitle(String channelId, String threadTs, String title) {
        Map<String, Object> payload = Map.of(
                "channel_id", channelId,
                "thread_ts", threadTs,
                "title", title
        );

        postToSlack("/assistant.threads.setTitle", payload,"스레드 제목 설정 성공", "스레드 제목 설정 실패");
    }


    private boolean isAichatterLinked(String slackUserId) {
        // TODO: DB 조회 실제 로직으로 대체
        return true;
    }

    private void postToSlack(String uri, Object payload, String successLog, String errorLog) {
        slackClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + botToken)
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        resp -> log.info("{}: {}", successLog, resp),
                        err -> log.error("{}: {}", errorLog, err.getMessage(), err)
                );
    }
}
