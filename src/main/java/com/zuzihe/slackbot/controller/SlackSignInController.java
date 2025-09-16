package com.zuzihe.slackbot.controller;

import com.zuzihe.slackbot.service.SlackSignInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
public class SlackSignInController {

    /// ******************** 테스트용 **************************************

    private final SlackSignInService slackSignInService;

    @GetMapping("/sign-in")
    public ResponseEntity<Void> startSignIn(
            @RequestParam String slack_user_id,
            @RequestParam String team_id) {
        return slackSignInService.startSignIn(slack_user_id, team_id);
    }

    @GetMapping("/sign-in/check")
    public ResponseEntity<String> checkMapping(
            @RequestParam String slack_user_id,
            @RequestParam(defaultValue = "team123") String team_id) {
        return slackSignInService.checkMapping(slack_user_id, team_id);
    }

    @PostMapping("/sign-in/complete")
    public ResponseEntity<String> completeSignIn(
            @RequestParam String token,
            @RequestParam String aichatterUserId) {
        return slackSignInService.completeSignIn(token, aichatterUserId);
    }
}

