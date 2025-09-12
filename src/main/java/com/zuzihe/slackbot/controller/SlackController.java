package com.zuzihe.slackbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuzihe.slackbot.service.JwtService;
import com.zuzihe.slackbot.service.SlackService;
import com.zuzihe.slackbot.service.SlackUserMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

    private final SlackService slackService;
    private final SlackUserMappingService mappingService;
    private final JwtService  jwtService;
    @GetMapping("/sign-in")
    public ResponseEntity<Void> startSignIn(
            @RequestParam String slack_user_id,
            @RequestParam String team_id
    ) {
        // JWT 발급 (slack_user_id, team_id 담음)
        String token = jwtService.issue(slack_user_id, team_id);

        // 8080 로그인 페이지로 Redirect
        URI redirect = URI.create("http://localhost:8080/api/login?token=" + token);
        return ResponseEntity.status(302).location(redirect).build();
    }
    // 매핑 조회 확인용
    @GetMapping("/sign-in/check")
    public ResponseEntity<String> checkMapping(
            @RequestParam String slack_user_id,
            @RequestParam(defaultValue = "team123") String team_id
    ) {
        String user = mappingService.findAichatterUser(team_id, slack_user_id);
        return ResponseEntity.ok("매핑 조회 결과: " + user);
    }
    @PostMapping("/sign-in/complete")
    public ResponseEntity<String> completeSignIn(
            @RequestParam String token,
            @RequestParam String aichatterUserId
    ) {
        Map<String, Object> claims = jwtService.verify(token);

        String slackUserId = (String) claims.get("slack_user_id");
        String teamId = (String) claims.get("team_id");

        mappingService.saveMapping(teamId, slackUserId, aichatterUserId);

        return ResponseEntity.ok("연동 성공: " + slackUserId + " → " + aichatterUserId);
    }

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
            } else if ("message".equals(eventType)) {
                // DM 메시지 처리
                String channelType = (String) event.get("channel_type");
                String text = (String) event.get("text");
                String userId = (String) event.get("user");

                // DM(im)인지 확인 + 봇 자신의 메시지가 아닌지 확인
                if  ("im".equals(channelType) &&
                        event.get("bot_id") == null &&      // 봇 메시지가 아님
                        text != null &&                     // 텍스트가 있음
                        !text.trim().isEmpty() &&           // 빈 메시지가 아님
                        userId != null) {
                    String channel = (String) event.get("channel");
                    String threadTs = (String) event.get("thread_ts");
                    String ts = (String) event.get("ts");
                    // AI 앱 스레드 응답 (thread_ts 사용)
                    slackService.handleDirectMessage(channel, text, userId, threadTs != null ? threadTs : ts);
                }

            }else if ("assistant_thread_started".equals(eventType)){
                Map<String, Object> assistantThread = (Map<String, Object>) event.get("assistant_thread");
                String userId = (String) assistantThread.get("user_id");
                String channelId = (String) assistantThread.get("channel_id");
                String threadTs = (String) assistantThread.get("thread_ts");

                // 새 어시스턴트 스레드가 시작되면 환영 메시지 발송
                slackService.sendWelcomeMessage(channelId, threadTs);
            }
        }
        return ResponseEntity.ok("OK");
    }

    //유저가 클릭할 수 있는 요소들....
    @PostMapping(value = "/interactive", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleInteractive(@RequestParam("payload") String payload) throws JsonProcessingException {
        log.info("Interactive payload 수신: {}", payload);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(payload);

        // 디버깅을 위한 전체 payload 로깅
        log.info("파싱된 payload: {}", root.toPrettyString());

        String type = root.get("type").asText();

        if ("block_actions".equals(type)) {
            String actionId = root.at("/actions/0/action_id").asText();
            String slackUserId = root.at("/user/id").asText();
            String channelId = root.at("/channel/id").asText();

            // thread_ts 가져오기 (환영 메시지의 스레드)
            String threadTs = null;
            if (root.has("message") && root.get("message").has("thread_ts")) {
                threadTs = root.get("message").get("thread_ts").asText();
            } else if (root.has("message") && root.get("message").has("ts")) {
                threadTs = root.get("message").get("ts").asText();
            }

            log.info("Action ID: {}, User ID: {}, Channel: {}, Thread: {}",
                    actionId, slackUserId, channelId, threadTs);

            // 환영 메시지 버튼 클릭 처리
            if (List.of("latest_trends", "b2b_social_media", "customer_feedback", "product_brainstorm")
                    .contains(actionId)) {
                slackService.handleButtonClick(channelId, actionId, threadTs);
            }
        }

        return ResponseEntity.ok("{}");
    }

}