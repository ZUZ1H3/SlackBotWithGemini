package com.zuzihe.slackbot.service;

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

    public ResponseEntity<String> handleEvent(Map<String, Object> payload) {
        String type = (String) payload.get("type");

        if ("url_verification".equals(type)) {
            return ResponseEntity.ok((String) payload.get("challenge"));
        }

        if ("event_callback".equals(type)) {
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String eventType = (String) event.get("type");

            switch (eventType) {
                case "app_home_opened" -> handleAppHomeOpened(event);
                case "assistant_thread_started" -> handleAssistantThread(event);
                case "app_mention" -> handleAppMention(event);
                case "message" -> handleMessage(event);
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
    }

    private void handleAppMention(Map<String, Object> event) {
        String text = (String) event.get("text");
        String channel = (String) event.get("channel");
        String prompt = geminiService.buildPrompt(text);

        geminiService.callGemini(prompt)
                .subscribe(answer -> slackWebClient.sendMessage(channel, answer));
    }

    private void handleMessage(Map<String, Object> event) {
        String channelType = (String) event.get("channel_type");
        String text = (String) event.get("text");
        String userId = (String) event.get("user");

        if ("im".equals(channelType) &&
                event.get("bot_id") == null &&
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