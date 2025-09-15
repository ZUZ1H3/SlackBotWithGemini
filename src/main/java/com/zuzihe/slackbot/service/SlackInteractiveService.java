package com.zuzihe.slackbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackInteractiveService {

    private final ObjectMapper objectMapper;
    private final SlackWebClient slackWebClient;
    private final GeminiService geminiService;
    private final List<String> welcomeButtonActions = List.of(
            "latest_trends", "b2b_social_media", "customer_feedback", "product_brainstorm"
    );

    public ResponseEntity<String> handleInteractive(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            log.info("Interactive payload 수신: {}", root.toPrettyString());

            String type = root.get("type").asText();

            if ("block_actions".equals(type)) {
                handleBlockActions(root);
            }

            return ResponseEntity.ok("{}");

        } catch (JsonProcessingException e) {
            log.error("페이로드 파싱 오류", e);
            return ResponseEntity.badRequest().body("Invalid payload");
        }
    }

    private void handleBlockActions(JsonNode root) {
        String actionId = root.at("/actions/0/action_id").asText();
        String channelId = root.at("/channel/id").asText();
        String threadTs = getThreadTsFromMessage(root);

        log.info("Action ID: {}, Channel: {}, Thread: {}", actionId, channelId, threadTs);

        if (welcomeButtonActions.contains(actionId)) {
            handleButtonClick(channelId, actionId, threadTs);
        }
    }

    private String getThreadTsFromMessage(JsonNode root) {
        JsonNode message = root.get("message");
        if (message != null) {
            return message.has("thread_ts")
                    ? message.get("thread_ts").asText()
                    : message.get("ts").asText();
        }
        return null;
    }

    @Async
    public void handleButtonClick(String channelId, String actionId, String threadTs) {
        // 1. 문서봇 버튼 클릭일 경우 → 고정 메시지 반환
        if ("customer_feedback".equals(actionId)) {
            slackWebClient.sendMessageWithThread(channelId, "*'지혜의 문서봇'이 선택되었습니다.*", threadTs);
            return;
        } else if ("product_brainstorm".equals(actionId)) {
            slackWebClient.sendMessageWithThread(channelId, "*'아이채터 정보봇'이 선택되었습니다.*", threadTs);
            return;
        }

        // 2. 프롬프트 버튼 → 기존처럼 AI 호출
        String question = getQuestionByActionId(actionId);
        if (question != null) {
            slackWebClient.sendMessageWithThread(channelId, "📍질문 :" + question, threadTs);

            String prompt = geminiService.buildPrompt(question);
            geminiService.callGemini(prompt).subscribe(
                    answer -> {
                        String safeText = convertMarkdownToMrkdwn(answer);
                        slackWebClient.sendMessageWithThread(channelId, safeText, threadTs);
                        log.info("버튼 클릭 AI 응답 완료 - Channel: {}", channelId);
                    },
                    error -> {
                        log.error("버튼 클릭 처리 실패", error);
                        slackWebClient.sendMessageWithThread(channelId, "일시적인 오류가 발생했습니다.", threadTs);
                    }
            );
        }
    }

    private String getQuestionByActionId(String actionId) {
        return switch (actionId) {
            case "latest_trends" -> "aichatter에 대해 알려주세요!!!";
            case "b2b_social_media" -> "문서봇을 만드는 방법이 무엇인강요?";
            default -> null;
        };
    }

    private String convertMarkdownToMrkdwn(String text) {
        return text
                .replaceAll("## ", "*")
                .replaceAll("\\*\\*(.*?)\\*\\*", "*$1*")
                .replaceAll("(?m)^- ", "• ")
                .replaceAll("(?m)^\\d+\\. ", "• ");
    }
}