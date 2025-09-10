package com.zuzihe.slackbot.service;

import com.zuzihe.slackbot.dto.SlackOAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackService {
    private final WebClient webClient;
    private final SlackWebClient slackWebClient; // Slack API 전용

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Async
    public void askAndSendToSlack(String channelId, String question) {
        String prompt = buildPrompt(question);
        callGemini(prompt).subscribe(answer -> {
            slackWebClient.sendMessage(channelId, answer);
        });
    }

    private Mono<String> callGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;

        return webClient.post()
                .uri(fullUrl)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractTextFromResponse);
    }

    //Gemini 응답에서 text만 추출
    private String extractTextFromResponse(Map<String, Object> response) {
        var candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates returned from Gemini API");
        }

        var content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("No parts in candidate content");
        }
        return (String) parts.get(0).get("text");
    }

    //Gemini prompt
    private String buildPrompt(String question) {

        return """
                당신의 이름은 aichatter 입니다. 답변하기 전 자기소개를 하세요.
                이모지를 사용하지 마세요.

            [질문]
            """ + question;
    }

    // AI 앱용 프롬프트
    private String buildAIChatPrompt(String question) {
        return """
                당신의 이름은 aichatter 입니다.
                문서 기반 질문 답변을 도와주는 AI 어시스턴트입니다.
                친절하고 정확한 답변을 제공해주세요.
                이모지는 적절히 사용하되 과도하지 않게 해주세요.

            [질문]
            """ + question;
    }
    public void publishHomeView(String userId) {
        slackWebClient.publishAppHome(userId); // WebClient 호출 위임
    }

    public void saveInstalledWorkspace(SlackOAuthResponse response) {
        if (response.getTeam() == null) {
            log.error("team 정보가 Slack 응답에 없습니다.");
            throw new IllegalStateException("Slack 응답에 team 정보 없음");
        }

        String teamId = response.getTeam().getId();
        String botToken = response.getAccess_token();

        // team_id와 bot_token 등을 DB에 저장
        // 예: workspace 테이블에 insert or update
    }

    public void updateHomeViewWithLoginLink(String userId, String loginUrl) {
        slackWebClient.updateHomeViewWithLoginLink(userId, loginUrl);
    }

    // AI 앱 첫 실행시 환영 메시지
    @Async
    public void handleThreadStart(String channelId, String userId, String threadTs) {
        try {
            log.info("AI 앱 스레드 시작 - Channel: {}, User: {}", channelId, userId);

            String welcomeMessage = """
                안녕하세요! aichatter입니다. 🤖
                
                저는 문서 기반 질문 답변을 도와드리는 AI 어시스턴트입니다.
                궁금한 것이 있으시면 언제든지 질문해 주세요!
                
                📝 예시 질문:
                • "회사 휴가 정책이 어떻게 되나요?"
                • "프로젝트 진행 절차를 알려주세요"
                • "시스템 사용법을 설명해주세요"
                """;

            // 환영 메시지를 스레드로 전송
            slackWebClient.sendMessageWithThread(channelId, welcomeMessage, threadTs);
            log.info("환영 메시지 전송 완료 - Channel: {}", channelId);
        } catch (Exception e) {
            log.error("스레드 시작 처리 중 오류 발생", e);
        }
    }
    // AI 앱 DM 메시지 처리 (핵심 기능)
    @Async
    public void handleDirectMessage(String channel, String text, String userId, String threadTs) {
        try {
            log.info("DM 메시지 수신 - Channel: {}, User: {}, Text: {}", channel, userId, text);

            // aichatter용 프롬프트 생성
            String prompt = buildAIChatPrompt(text);

            // Gemini API 호출 후 스레드로 응답
            callGemini(prompt).subscribe(
                    answer -> {
                        // 스레드 타임스탬프와 함께 응답 전송
                        slackWebClient.sendMessageWithThread(channel, answer, threadTs);
                        log.info("AI 응답 전송 완료 - Channel: {}", channel);
                    },
                    error -> {
                        log.error("Gemini API 호출 실패", error);
                        slackWebClient.sendMessageWithThread(channel, "죄송합니다. 일시적인 오류가 발생했습니다. 다시 시도해 주세요.", threadTs);
                    }
            );
        } catch (Exception e) {
            log.error("DM 메시지 처리 중 오류 발생", e);
            slackWebClient.sendMessageWithThread(channel, "죄송합니다. 처리 중 오류가 발생했습니다.", threadTs);
        }
    }

}
