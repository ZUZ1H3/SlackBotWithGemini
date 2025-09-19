package com.zuzihe.slackbot.link.account.web;

import com.zuzihe.slackbot.link.account.service.SlackSignInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * slack 유저 계정과 aichatter 계정 연동을 위한 클래스
 */
@Slf4j
@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
public class SlackSignInController {

    /// ******************** 테스트용 **************************************

    private final SlackSignInService slackSignInService;

    // aichatter 연동 요청
    @GetMapping("/sign-in")
    public ResponseEntity<Void> startSignIn(
            @RequestParam String slack_user_id,
            @RequestParam String team_id) {
        return slackSignInService.startSignIn(slack_user_id, team_id);
    }

    // 연동 결과  조회
    @GetMapping("/sign-in/check")
    public ResponseEntity<String> checkMapping(
            @RequestParam String slack_user_id,
            @RequestParam(defaultValue = "team123") String team_id) {
        return slackSignInService.checkMapping(slack_user_id, team_id);
    }

    // 연동 성공 시 redirect
    @PostMapping("/sign-in/complete")
    public ResponseEntity<String> completeSignIn(
            @RequestParam String token,
            @RequestParam String aichatterUserId) {
        return slackSignInService.completeSignIn(token, aichatterUserId);
    }
}

