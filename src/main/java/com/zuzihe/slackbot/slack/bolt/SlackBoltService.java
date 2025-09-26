package com.zuzihe.slackbot.slack.bolt;

import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import com.zuzihe.slackbot.llm.GeminiService;
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

            // 1. 즉시 "답변 생성 중" 메시지 전송
           slackBoltClient.setThinkingStatus(channel, threadTs);

            // 2. LLM 호출 및 메시지 업데이트
            String prompt = geminiService.buildPrompt(text);
            geminiService.callGemini(prompt).subscribe(
                    answer -> {
                        String safe = convertMarkdownToMrkdwn(answer);
                        // 기존 "답변 생성 중" 메시지를 실제 답변으로 업데이트
                        slackBoltClient.sendMessageWithThread(channel, safe, threadTs);
                        log.info("[Bolt] DM 응답 업데이트 완료 - Channel: {}", channel);
                    },
                    error -> {
                        log.error("[Bolt] Gemini 호출 실패", error);
                        // 오류 발생 시에도 메시지 업데이트
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
    public void handleAppMention(AppMentionEvent ev) {
        String parentTs = ev.getThreadTs() != null ? ev.getThreadTs() : ev.getTs();
        String channel = ev.getChannel();
        String text = ev.getText();

        try {
            log.info("[Bolt] App Mention 수신 - Channel: {}, Text: {}", channel, text);

            // 1. 즉시 "답변 생성 중" 메시지 전송
           slackBoltClient.setThinkingStatus(channel, parentTs);

            // 2. LLM 호출 및 메시지 업데이트
            String prompt = geminiService.buildPrompt(text);
            geminiService.callGemini(prompt).subscribe(
                    answer -> {
                        String safe = convertMarkdownToMrkdwn(answer);
                        // 기존 "답변 생성 중" 메시지를 실제 답변으로 업데이트
                        slackBoltClient.sendMessageWithThread(channel, safe, parentTs);
                        log.info("[Bolt] App Mention 응답 업데이트 완료 - Channel: {}", channel);
                    },
                    error -> {
                        log.error("[Bolt] App Mention Gemini 호출 실패", error);
                        // 오류 발생 시에도 메시지 업데이트
                        slackBoltClient.sendMessageWithThread(channel, "일시적인 오류가 발생했습니다.", parentTs);
                    }
            );
        } catch (Exception e) {
            log.error("[Bolt] App Mention 처리 중 오류", e);
            slackBoltClient.sendMessageWithThread(channel, "처리 중 오류가 발생했습니다.", parentTs);
        }
    }

    @Async
    public void openNewChatWithUser(String userId, String chatRoomId) {
        log.info("[Bolt] 새로운 채팅 시작 - UserId: {}", userId);
        // 사용자와의 DM 채널 열고 환영 메시지 전송
        slackBoltClient.openConversationAndSendWelcomeMessageWithChatRoomId(userId, chatRoomId);
    }
}
