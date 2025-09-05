package com.zuzihe.slackbot.controller;

import com.zuzihe.slackbot.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

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
                String user = (String) event.get("user"); // 메시지를 보낸 사용자 ID (예: U12345678)

                // Gemini API 호출 + Slack 메시지 전송 로직 수행
                slackService.askAndSendToSlack(channel, text);
            }
        }
        return ResponseEntity.ok("OK");
    }
}