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
                        slackWebClient.sendMessageWithThread(channel, answer, threadTs);
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
