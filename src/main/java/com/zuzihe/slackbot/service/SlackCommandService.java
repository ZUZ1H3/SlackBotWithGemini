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
public class SlackCommandService {

    private final GeminiService geminiService;
    private final SlackWebClient slackWebClient;

    public ResponseEntity<String> handleCommand(Map<String, String> params) {
        String userId = params.get("user_id");
        String channelId = params.get("channel_id");
        String question = params.get("text");

        askAndSendToSlack(channelId, question);

        return ResponseEntity.ok(userId + "님! 질문을 받았습니다! 잠시만요…");
    }

    @Async
    public void askAndSendToSlack(String channelId, String question) {
        String prompt = geminiService.buildPrompt(question);
        geminiService.callGemini(prompt).subscribe(answer -> {
            slackWebClient.sendMessage(channelId, answer);
        });
    }
}