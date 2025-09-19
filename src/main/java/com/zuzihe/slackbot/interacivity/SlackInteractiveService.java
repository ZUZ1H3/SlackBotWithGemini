package com.zuzihe.slackbot.interacivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuzihe.slackbot.message.infra.GeminiService;
import com.zuzihe.slackbot.slack.http.global.infra.SlackWebClient;
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

    private final List<String> welcomeButtonActions = List.of( // Slack Block Kit 버튼(action_id) 목록
            "ask_about_aichatter",
            "ask_how_to_build_docbot",
            "select_docbot",
            "select_infobot"
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

    /**
     * block_actions 이벤트 처리
     * - 어떤 버튼(action_id)이 눌렸는지 식별
     * - 채널/스레드 정보 추출 후 버튼 클릭 핸들러 호출
     */
    private void handleBlockActions(JsonNode root) {
        String actionId = root.at("/actions/0/action_id").asText();
        String channelId = root.at("/channel/id").asText();
        String threadTs = getThreadTsFromMessage(root);

        log.info("Action ID: {}, Channel: {}, Thread: {}", actionId, channelId, threadTs);

        if (welcomeButtonActions.contains(actionId)) {
            handleButtonClick(channelId, actionId, threadTs);
        }
    }

    /**
     * 메시지 객체에서 thread_ts 또는 ts 추출
     * - thread_ts 있으면 스레드 답글
     * - 없으면 일반 메시지
     */
    private String getThreadTsFromMessage(JsonNode root) {
        JsonNode message = root.get("message");
        if (message != null) {
            return message.has("thread_ts")
                    ? message.get("thread_ts").asText()
                    : message.get("ts").asText();
        }
        return null;
    }

    /**
     * 버튼 클릭 처리
     * - select_docbot / select_infobot: 고정 메시지 반환
     * - ask_about_aichatter / ask_how_to_build_docbot: Gemini AI 호출 후 응답 반환
     * - lack 응답 타임아웃(3초) 문제를 피하기 위해 비동기 실행
     */
    @Async
    public void handleButtonClick(String channelId, String actionId, String threadTs) {
        // 1. 문서봇 버튼 클릭일 경우 → 고정 메시지 반환
        if ("select_docbot".equals(actionId)) {
            slackWebClient.sendMessageWithThread(channelId, "*'지혜의 문서봇'이 선택되었습니다.*", threadTs);
            return;
        } else if ("select_infobot".equals(actionId)) {
            slackWebClient.sendMessageWithThread(channelId, "*'아이채터 정보봇'이 선택되었습니다.*", threadTs);
            return;
        }

        // 2. 질문 버튼 → Gemini 호출
        String question = getQuestionByActionId(actionId);
        if (question != null) {
            // 우선 질문 내용을 Slack에 표시
            slackWebClient.sendMessageWithThread(channelId, "📍질문 :" + question, threadTs);

            // Gemini 프롬프트 생성 + 호출
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

    /**
     * action_id → 질문 텍스트 매핑
     * - 버튼 클릭 시 어떤 질문을 보낼지 결정
     */
    private String getQuestionByActionId(String actionId) {
        return switch (actionId) {
            case "ask_about_aichatter" -> "aichatter에 대해 알려주세요!!!";
            case "ask_how_to_build_docbot" -> "문서봇을 만드는 방법이 무엇인강요?";
            default -> null;
        };
    }

    /**
     * Markdown → Slack mrkdwn 변환
     * - LLM 답변은 보통 Markdown으로 오기 때문에 Slack에 맞춰 변환 필요
     * - ## 헤더, **볼드**, 리스트(-, 숫자) 등을 mrkdwn 문법으로 치환
     */
    private String convertMarkdownToMrkdwn(String text) {
        return text
                .replaceAll("## ", "*")
                .replaceAll("\\*\\*(.*?)\\*\\*", "*$1*")
                .replaceAll("(?m)^- ", "• ")
                .replaceAll("(?m)^\\d+\\. ", "• ");
    }
}