package com.zuzihe.slackbot.service;

import com.zuzihe.slackbot.dto.SlackOAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackService {
    private final SlackWebClient slackWebClient; // Slack API 전용

   private final GeminiService geminiService;

    @Async
    public void askAndSendToSlack(String channelId, String question) {
        String prompt = geminiService.buildPrompt(question);
        geminiService.callGemini(prompt).subscribe(answer -> {
            slackWebClient.sendMessage(channelId, answer);
        });
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



    @Async
    public void sendWelcomeMessage(String channelId, String threadTs) {
        try {
            // 환영 메시지 전송
            slackWebClient.sendWelcomeMessageWithButtons(channelId, threadTs);
        } catch (Exception e) {
            log.error("환영 메시지 전송 중 오류 발생 - Channel: {}", channelId, e);
        }
    }

    // 버튼 클릭 처리 메서드 추가
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

    private String convertMarkdownToMrkdwn(String text) {
        return text
                .replaceAll("## ", "*")         // 제목 → 굵게
                .replaceAll("\\*\\*(.*?)\\*\\*", "*$1*") // 굵게
                .replaceAll("(?m)^- ", "• ")   // 리스트
                .replaceAll("(?m)^\\d+\\. ", "• "); // 번호 리스트
    }

    private String getQuestionByActionId(String actionId) {
        return switch (actionId) {
            case "latest_trends" -> "aichatter에 대해 알려주세요!!!";
            case "b2b_social_media" -> "문서봇을 만드는 방법이 무엇인강요?";
            default -> null;
        };
    }
    // AI 앱 DM 메시지 처리
    @Async
    public void handleDirectMessage(String channel, String text, String userId, String threadTs) {
        try {
            log.info("DM 메시지 수신 - Channel: {}, User: {}, Text: {}", channel, userId, text);

            // aichatter용 프롬프트 생성
            String prompt = geminiService.buildPrompt(text);

            // Gemini API 호출 후 스레드로 응답
            geminiService.callGemini(prompt).subscribe(
                    answer -> {
                        // 스레드 타임스탬프와 함께 응답 전송
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
}
