package com.zuzihe.slackbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zuzihe.slackbot.dto.SlackOAuthResponse;
import com.zuzihe.slackbot.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {
    @Value("${slack.client-id}")
    private String slackClientId;

    @Value("${slack.client-secret}")
    private String slackClientSecret;

    private final SlackService slackService;

    // 슬래시 커맨드 /aichatter
    @PostMapping(value = "/commands", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleCommand(
            @RequestParam Map<String, String> params
    ) {
        String userId = params.get("user_id");
        String channelId = params.get("channel_id");
        String question = params.get("text");

        // 비동기로 처리 (3초 안에 OK만 보내기)
        slackService.askAndSendToSlack(channelId, question);

        return ResponseEntity.ok(userId +"님! 질문을 받았습니다! 잠시만요…");
    }

    // 슬랙의 이벤트 요청을 받는 엔드포인트
    // Mentions(@aichatter), App Home, 버튼 클릭 등 모든 이벤트는 여기로 POST
    @PostMapping("/events")
    public ResponseEntity<String> handleEvent(
            @RequestBody Map<String, Object> payload
    ) {
        // URL 검증 처리
        if ("url_verification".equals(payload.get("type"))) {
            // Slack에게 challenge 문자열 그대로 응답해줘야 검증 통과
            return ResponseEntity.ok((String) payload.get("challenge"));
        }

        // 슬랙에서 발생한 이벤트 종류를 받음
        if ("event_callback".equals(payload.get("type"))) {
            Map<String, Object> event = (Map<String, Object>) payload.get("event"); // 이벤트 내용은 payload["event"] 안에 있음

            String eventType = (String) event.get("type"); // 이벤트 종류 추출 (예: app_mention, app_home_opened 등)

            // 멘션(@aichatter)
            if ("app_mention".equals(eventType)) {
                String text = (String) event.get("text"); // 사용자가 입력한 전체 메시지 텍스트
                String channel = (String) event.get("channel"); // 메시지가 발생한 채널 ID (예: C12345678)
                String userId = (String) event.get("user"); // 메시지를 보낸 사용자 ID (예: U12345678)

                // Gemini API 호출 + Slack 메시지 전송 로직 수행
                slackService.askAndSendToSlack(channel, text);
            } else if ("app_home_opened".equals(eventType)) {
                String userId = (String) event.get("user"); // 홈탭을 연 사용자 ID

                // 홈 탭 뷰 표시 (Slack Web API - views.publish)
                slackService.publishHomeView(userId);
            }
        }
        return ResponseEntity.ok("OK");
    }

    //
    @GetMapping("/oauth/callback")
    public ResponseEntity<String> handleSlackOAuthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) throws JsonProcessingException {
        log.info("Slack callback 도착! code = {}, state = {}", code, state);

        WebClient webClient = WebClient.create();
        String rawJson = webClient.post()
                .uri("https://slack.com/api/oauth.v2.access")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("code=" + code +
                        "&client_id=" + slackClientId +
                        "&client_secret=" + slackClientSecret +
                        "&redirect_uri=https://949ac40aa5c9.ngrok-free.app/slack/oauth/callback")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Slack OAuth 응답 원문:\n{}", rawJson);

        // JSON 문자열 → DTO로 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        SlackOAuthResponse response = objectMapper.readValue(rawJson, SlackOAuthResponse.class);

        log.info("🔍 SlackOAuthResponse 매핑 결과: {}", response);

        if (!response.isOk()) {
            return ResponseEntity.status(500).body("Slack OAuth 실패: " + response.getError());
        }

        slackService.saveInstalledWorkspace(response);

        return ResponseEntity.ok("Slack 앱 설치 완료!");
    }

    @PostMapping(value = "/interactive", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleInteractive(@RequestParam("payload") String payload) throws JsonProcessingException {
        log.info("nteractive payload 수신: {}", payload);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(payload);

        // 디버깅을 위한 전체 payload 로깅
        log.info("파싱된 payload: {}", root.toPrettyString());

        String actionId = root.at("/actions/0/action_id").asText();
        String slackUserId = root.at("/user/id").asText();

        log.info("Action ID: {}, User ID: {}", actionId, slackUserId);

        if ("go_to_login".equals(actionId)) {
            log.info("로그인 버튼 클릭 처리 시작");

            // 로그인 URL 구성
            String loginUrl = String.format(
                    "http://mcloudoc.aichatter.net:6500/sign-in?slack_user_id=%s", slackUserId
            );

            log.info("생성된 로그인 URL: {}", loginUrl);

            // 홈탭을 업데이트하는 방식으로 변경
            try {
                slackService.updateHomeViewWithLoginLink(slackUserId, loginUrl);
                log.info("홈탭 업데이트 완료");
                return ResponseEntity.ok(""); // 빈 응답
            } catch (Exception e) {
                log.error("홈탭 업데이트 실패", e);

                // 에러 메시지를 ephemeral로 응답
                ObjectNode response = mapper.createObjectNode();
                response.put("response_type", "ephemeral");
                response.put("text", "로그인 링크 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");

                return ResponseEntity.ok(response.toString());
            }
        }

        // 다른 버튼 액션들 처리
        log.info("처리되지 않은 action_id: {}", actionId);
        return ResponseEntity.ok("{}");
    }

}