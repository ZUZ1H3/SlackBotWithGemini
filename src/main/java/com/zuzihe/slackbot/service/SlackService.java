package com.zuzihe.slackbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SlackService {
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public Mono<String> ask(String question) {
        String prompt = buildPrompt(question);
        return callGemini(prompt);
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
                당신의 이름은 aichatter 봇입니다. 답변하기 전 자기소개를 하세요.
                당신의 목적은 정보를 보기좋게 주는 것이 아닌, 사람과의 대화입니다.
                일상에서 말하듯 대화하세요.

            [질문]
            """ + question;
    }
}
