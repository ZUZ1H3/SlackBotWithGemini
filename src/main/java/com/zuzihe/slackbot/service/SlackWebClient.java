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
                        resp -> System.out.println("홈 탭 전송 성공: " + resp),
                        err -> System.err.println("홈 탭 전송 실패: " + err.getMessage())
                );
    }
    private List<Map<String, Object>> getlinkedBlocks() {
        return List.of(
                // 상단 인사
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "👋 *안녕하세요, aichatter입니다.* 더 빠르고 효율적으로 업무를 도와드립니다.\n아래 명령어를 활용해보세요."
                )),
                // 도움말 한 줄
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "• `/aichatter-help` – 사용 가능한 기능 도움말"
                )),

                Map.of("type", "divider"),

                // 새로운 대화 시작하기
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "*새로운 대화 시작하기*\n오른쪽 상단의 `+ 새 채팅` 버튼을 눌러주세요."
                )),

                // 지원 문의
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "*지원 문의*\n추가 문의는 <mailto:support@aichatter.com|support@aichatter.com> 으로 연락해주세요."
                )),

                // 나의 문서봇 제목
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "*나의 문서봇*"
                )),
                Map.of("type", "divider"),

                // 문서봇 아이템 1 (예시)
                Map.of("type", "section",
                        "text", Map.of(
                                "type", "mrkdwn",
                                "text", "*apispec-bot · aichatter*\n최근 대화한 날짜 · *5일 전*    _NEW_"
                        ),
                        "accessory", Map.of(
                                "type", "button",
                                "text", Map.of("type", "plain_text", "text", "열기"),
                                "action_id", "open_docbot_apispec",
                                "value", "docbot_id_1"
                        )
                ),

                // 문서봇 아이템 2 (예시)
                Map.of("type", "section",
                        "text", Map.of(
                                "type", "mrkdwn",
                                "text", "*영업지원 문서봇*\n최근 대화한 날짜 · *2025-07-31 09:15*"
                        ),
                        "accessory", Map.of(
                                "type", "button",
                                "text", Map.of("type", "plain_text", "text", "열기"),
                                "action_id", "open_docbot_sales",
                                "value", "docbot_id_2"
                        )
                ),

                Map.of("type", "divider")
        );
    }

    private List<Map<String, Object>> getLinkedBlocksDirectChat() {
        return List.of(
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "*aichatter 연동 완료!* 🎉\n\n문서봇을 선택하면 바로 채팅창이 열립니다:"
                )),

                Map.of("type", "divider"),

                Map.of(
                        "type", "section",
                        "text", Map.of(
                                "type", "mrkdwn",
                                "text", "🤖 *apiSpec bot* `NEW`\n_API 명세서 관련 질문에 최적화_"
                        ),
                        "accessory", Map.of(
                                "type", "button",
                                "text", Map.of("type", "plain_text", "text", "질문하기"),
                                "style", "primary",
                                "action_id", "open_chat_modal_apispec"
                        )
                ),

                Map.of(
                        "type", "section",
                        "text", Map.of(
                                "type", "mrkdwn",
                                "text", "🧪 *최근용 test*\n_14개 소스 • 5월 전 업데이트_"
                        ),
                        "accessory", Map.of(
                                "type", "button",
                                "text", Map.of("type", "plain_text", "text", "질문하기"),
                                "action_id", "open_chat_modal_recent"
                        )
                ),

                Map.of(
                        "type", "section",
                        "text", Map.of(
                                "type", "mrkdwn",
                                "text", "🏔️ *강원도 문서봇*\n_3개 소스 • 2025-07-31 업데이트_"
                        ),
                        "accessory", Map.of(
                                "type", "button",
                                "text", Map.of("type", "plain_text", "text", "질문하기"),
                                "action_id", "open_chat_modal_gangwon"
                        )
                ),

                Map.of("type", "context", "elements", List.of(
                        Map.of("type", "mrkdwn", "text", "💡 `/aichatter 질문내용`으로도 바로 사용 가능 • 문의: @지원봇")
                ))
        );
    }
    private List<Map<String, Object>> getUnlinkedBlocks() {
        return List.of(
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "* aichatter를 슬랙에서 사용하려면 먼저 계정을 연동해주세요.*"
                )),
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", """
                        aichatter는 사내 문서를 바탕으로 질문에 답하고 요약해주는 AI 문서봇입니다.  
                        슬랙에 연동하면 다음 기능을 사용할 수 있어요.

                        • `/aichatter` 명령어로 바로 질문  
                        • 문서봇 선택 후 대화형 질의  
                        • 질문 기록 자동 저장  
                        • 사내 데이터 기반 답변 제공
                        """
                )),
                Map.of("type", "divider"),
                Map.of("type", "actions", "elements", List.of(
                        Map.of(
                                "type", "button",
                                "text", Map.of("type", "plain_text", "text", "🔗 aichatter 로그인하기"),
                                "style", "primary",
                                //"url", "http://mcloudoc.aichatter.net:6500/sign-in",
                                "action_id", "go_to_login"
                        )
                )),
                Map.of("type", "context", "elements", List.of(
                        Map.of("type", "mrkdwn", "text", "_계정 연동 후 이 홈탭이 자동으로 업데이트됩니다._")
                ))
        );
    }


    private boolean isAichatterLinked(String slackUserId) {
        // TODO: DB 조회 실제 로직으로 대체
        return false;
    }

    // 로그인 링크가 포함된 홈탭 업데이트
    public void updateHomeViewWithLoginLink(String userId, String loginUrl) {
        log.info("로그인 링크로 홈탭 업데이트 - 사용자: {}, URL: {}", userId, loginUrl);

        Map<String, Object> view = Map.of(
                "type", "home",
                "blocks", getLoginLinkBlocks(loginUrl)
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
                        resp -> {
                            log.info("로그인 링크 홈탭 업데이트 성공: {}", resp);
                        },
                        err -> {
                            log.error("로그인 링크 홈탭 업데이트 실패: {}", err.getMessage(), err);
                        }
                );
    }

    // 로그인 링크를 포함한 블록들
    private List<Map<String, Object>> getLoginLinkBlocks(String loginUrl) {
        return List.of(
                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "🔗 *aichatter 로그인 페이지*"
                )),

                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "아래 링크를 클릭하여 aichatter 계정으로 로그인해주세요:"
                )),

                Map.of("type", "section", "text", Map.of(
                        "type", "mrkdwn",
                        "text", "👉 <" + loginUrl + "|aichatter 로그인하기>"
                )),

                Map.of("type", "divider"),

                Map.of("type", "context", "elements", List.of(
                        Map.of("type", "mrkdwn", "text", "_로그인 완료 후 이 페이지가 자동으로 업데이트됩니다._")
                )),

                // 다시 시도 버튼
                Map.of("type", "actions", "elements", List.of(
                        Map.of(
                                "type", "button",
                                "text", Map.of("type", "plain_text", "text", "🔄 새로고침"),
                                "action_id", "refresh_home_tab",
                                "style", "primary"
                        )
                ))
        );
    }
}
