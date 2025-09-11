package com.zuzihe.slackbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuzihe.slackbot.dto.SlackOAuthResponse;
import com.zuzihe.slackbot.service.SlackService;
import com.zuzihe.slackbot.service.StateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackInstallController {
    @Value("${slack.client-id}")
    private String slackClientId;

    @Value("${slack.client-secret}")
    private String slackClientSecret;

    @Value("${slack.redirect-uri}")
    private String redirectUri;

    private final SlackService slackService;
    private final StateService stateService;

    @GetMapping("/oauth/install")
    public ResponseEntity<Void> start() {
        String state = stateService.issue();
        String url = "https://slack.com/oauth/v2/authorize"
                + "?client_id=" + slackClientId
                + "&scope=commands,chat:write"
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&state=" + state;

        URI auth = URI.create(url);

        return ResponseEntity.status(302).location(auth).build();
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<String> handleSlackOAuthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) throws JsonProcessingException {
        log.info("Slack callback 도착! code = {}, state = {}", code, state);

        WebClient webClient = WebClient.create();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", slackClientId);
        form.add("client_secret", slackClientSecret);
        form.add("redirect_uri", redirectUri);

        String rawJson = webClient.post()
                .uri("https://slack.com/api/oauth.v2.access")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Slack OAuth 응답 원문:\n{}", rawJson);

        // JSON 문자열 → DTO로 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        SlackOAuthResponse response = objectMapper.readValue(rawJson, SlackOAuthResponse.class);

        log.info("SlackOAuthResponse 매핑 결과: {}", response);

        if (!response.isOk()) {
            return ResponseEntity.status(500).body("Slack OAuth 실패: " + response.getError());
        }

        slackService.saveInstalledWorkspace(response);

        return ResponseEntity.ok("Slack 앱 설치 완료!");
    }
}
