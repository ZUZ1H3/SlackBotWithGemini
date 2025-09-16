package com.zuzihe.slackbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zuzihe.slackbot.service.SlackOAuthService;
import com.zuzihe.slackbot.service.StateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return slackOAuthService.getInstallRedirect(stateService.issue());
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
        return slackOAuthService.handleCallback(code, state);
    }
}
