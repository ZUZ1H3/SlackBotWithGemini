package com.zuzihe.slackbot.slack.http.install.app.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zuzihe.slackbot.global.exception.CustomException;
import com.zuzihe.slackbot.slack.http.install.app.service.SlackOAuthService;
import com.zuzihe.slackbot.slack.http.install.app.service.StateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Slack App 설치에 사용
 */
@Slf4j
@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
public class SlackOauthController {
    private final StateService stateService;
    private final SlackOAuthService slackOAuthService;

    // 사용자가 앱 설치를 시작하면 Slack 인증 페이지로 리다이렉트
    @GetMapping("/oauth/install")
    public ResponseEntity<Void> start() {
        String installRedirect = slackOAuthService.getInstallRedirect(stateService.issue());

        return ResponseEntity
                .status(302)
                .location(URI.create(installRedirect))
                .build();
    }


    /* Slack OAuth 인증 완료 후 콜백을 받는 엔드포인트
    Slack에서 code, state 파라미터를 담아 호출함
    code: 액세스 토큰 교환용 인증 코드
    state: CSRF 방지를 위해 발급했던 임의 문자열
    이 엔드포인트에서 Slack API (/oauth.v2.access)에 code를 교환하여 bot token 발급받음 */
    @GetMapping("/oauth/callback")
    public ResponseEntity<String> handleSlackOAuthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) throws JsonProcessingException {
        try {
            String body = slackOAuthService.handleCallback(code, state);
            return ResponseEntity.ok(body);
        } catch (CustomException e) {
            String body = e.getMessage();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body);
        }
    }
}
