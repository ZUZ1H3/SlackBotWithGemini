package com.zuzihe.slackbot.slack.bolt;

import com.slack.api.model.event.MessageEvent;
import com.zuzihe.slackbot.message.infra.GeminiService;
import com.zuzihe.slackbot.slack.bolt.infra.SlackBoltClient;
import com.zuzihe.slackbot.slack.http.global.infra.SlackWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackBoltService {

    private final GeminiService geminiService;
    private final SlackBoltClient slackBoltClient;
    private final SlackWebClient slackWebClient;

    @Async
    public void handleDirectMessage(MessageEvent ev) {
        String channel = ev.getChannel();
        String text = ev.getText();
        String userId = ev.getUser();
        String threadTs = ev.getThreadTs() != null ? ev.getThreadTs() : ev.getTs();

        try {
            log.info("[Bolt] DM 수신 - Channel: {}, User: {}, Text: {}", channel, userId, text);
            String prompt = geminiService.buildPrompt(text);
            geminiService.callGemini(prompt).subscribe(
                    answer -> {
                        String safe = convertMarkdownToMrkdwn(answer);
                        slackBoltClient.sendMessageWithThread(channel, safe, threadTs);
                        log.info("[Bolt] DM 응답 전송 완료 - Channel: {}", channel);
                    },
                    error -> {
                        log.error("[Bolt] Gemini 호출 실패", error);
                        slackBoltClient.sendMessageWithThread(channel, "일시적인 오류가 발생했습니다.", threadTs);
                    }
            );
        } catch (Exception e) {
            log.error("[Bolt] DM 처리 중 오류", e);
            slackBoltClient.sendMessageWithThread(channel, "처리 중 오류가 발생했습니다.", threadTs);
        }
    }

    // mrkdwn은 슬랙 전용 문법
    private String convertMarkdownToMrkdwn(String text) {
        return text
                .replaceAll("## ", "*")
                .replaceAll("\\*\\*(.*?)\\*\\*", "*$1*")
                .replaceAll("(?m)^- ", "• ")
                .replaceAll("(?m)^\\d+\\. ", "• ");
    }

    @Async
    public void handleAppHomeOpened(String userId) {
        // Reuse existing WebClient blocks for now
        slackBoltClient.publishAppHome(userId);
    }

    @Async
    public void handleAssistantThread(String channelId, String threadTs) {
        slackBoltClient.sendWelcomeMessageWithButtons(channelId, threadTs);
    }

    @Async
    public void handleAppMention(String channel, String text, String parentTs) {
        String prompt = geminiService.buildPrompt(text);
        geminiService.callGemini(prompt)
                .subscribe(answer -> slackBoltClient.sendMessageWithThread(channel, answer, parentTs));
    }
}
