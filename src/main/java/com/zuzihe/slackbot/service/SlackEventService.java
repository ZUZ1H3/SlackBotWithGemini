package com.zuzihe.slackbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackEventService {
    ///  이벤트 타입 더 많아지면 Processor 클래스로 따로 나누는 것 고려
    private final SlackWebClient slackWebClient;
    private final GeminiService geminiService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<String> handleEvent(Map<String, Object> payload) {
        String type = (String) payload.get("type");

        if ("url_verification".equals(type)) { //Slack이 Event Subscription을 등록할 때 처음 보내는 요청
            return ResponseEntity.ok((String) payload.get("challenge")); //서버는 challenge 문자열만 그대로 응답해야 Slack에서 URL을 유효하다고 인정함
        }

        if ("event_callback".equals(type)) {
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String eventType = (String) event.get("type");

            switch (eventType) {
                case "app_home_opened" -> handleAppHomeOpened(event); //홈 탭 열었을 때 (홈 UI 초기화, 안내 메시지 표시 등).
                case "assistant_thread_started" -> handleAssistantThread(event); //새 채팅방 열었을 때
                case "app_mention" -> handleAppMention(event); //@봇이름 으로 멘션 호출 되었을 때
                case "message" -> handleMessage(event); //DM이나 채널에서 메시지 보냈을 때
                default -> log.warn("지원하지 않는 이벤트 타입: {}", eventType);
            }
        }
        return ResponseEntity.ok("OK");
    }

    private void handleAppHomeOpened(Map<String, Object> event) {
        String userId = (String) event.get("user");
        slackWebClient.publishAppHome(userId);
    }

    private void handleAssistantThread(Map<String, Object> event) {
        Map<String, Object> assistantThread = (Map<String, Object>) event.get("assistant_thread");
        String channelId = (String) assistantThread.get("channel_id");
        String threadTs = (String) assistantThread.get("thread_ts");
        slackWebClient.sendWelcomeMessageWithButtons(channelId, threadTs);
        slackWebClient.setThreadTitle(channelId, threadTs, "기본 제목입니다.");
    }

    private void handleAppMention(Map<String, Object> event) {
        String text = (String) event.get("text");
        String channel = (String) event.get("channel");
        String prompt = geminiService.buildPrompt(text);

        geminiService.callGemini(prompt)
                .subscribe(answer -> slackWebClient.sendMessage(channel, answer));
    }

    private void handleMessage(Map<String, Object> event) {
        try {
            log.info("Slack Event Payload: {}", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to log event", e);
        }
        String channelType = (String) event.get("channel_type");
        String text = (String) event.get("text");
        String userId = (String) event.get("user");

        if ("im".equals(channelType) && // 보낸 메시지가 DM일 경우 channelType은 "im"
                event.get("bot_id") == null && //봇 자기 자신이 보낸 메시지일 경우에는 무시
                text != null && !text.trim().isEmpty() &&
                userId != null) {

            String channel = (String) event.get("channel");
            String threadTs = (String) event.get("thread_ts");
            String ts = (String) event.get("ts");

            handleDirectMessage(channel, text, userId, threadTs != null ? threadTs : ts);
        }
    }

    @Async
    public void handleDirectMessage(String channel, String text, String userId, String threadTs) {
        try {
            log.info("DM 메시지 수신 - Channel: {}, User: {}, Text: {}", channel, userId, text);

            String prompt = geminiService.buildPrompt(text);
            geminiService.callGemini(prompt).subscribe(
                    answer -> {
                        String safeText = convertMarkdownToMrkdwn(answer);
                        slackWebClient.sendMessageWithThread(channel, safeText, threadTs);
                        log.info("AI 응답 전송 완료 - Channel: {}", channel);
                    },
                    error -> {
                        log.error("Gemini API 호출 실패", error);
                        slackWebClient.sendMessageWithThread(channel, "일시적인 오류가 발생했습니다.", threadTs);
                    }
            );
        } catch (Exception e) {
            log.error("DM 메시지 처리 중 오류 발생", e);
            slackWebClient.sendMessageWithThread(channel, "처리 중 오류가 발생했습니다.", threadTs);
        }
    }

    private String convertMarkdownToMrkdwn(String text) {
        return text
                .replaceAll("## ", "*")
                .replaceAll("\\*\\*(.*?)\\*\\*", "*$1*")
                .replaceAll("(?m)^- ", "• ")
                .replaceAll("(?m)^\\d+\\. ", "• ");
    }
}