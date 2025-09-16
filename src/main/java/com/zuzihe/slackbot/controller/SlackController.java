package com.zuzihe.slackbot.controller;

import com.zuzihe.slackbot.service.SlackCommandService;
import com.zuzihe.slackbot.service.SlackEventService;
import com.zuzihe.slackbot.service.SlackInteractiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
public class SlackController {

    private final SlackCommandService slackCommandService;
    private final SlackEventService slackEventService;
    private final SlackInteractiveService slackInteractiveService;

    // Slack Slash Command 처리
    @PostMapping(value = "/commands", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleCommand(@RequestParam Map<String, String> params) {
        return slackCommandService.handleCommand(params);
    }

    // Slack Event API 처리
    @PostMapping("/events")
    public ResponseEntity<String> handleEvent(@RequestBody Map<String, Object> payload) {
        return slackEventService.handleEvent(payload);
    }

    // Slack Interactive Component 처리
    @PostMapping(value = "/interactive", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleInteractive(@RequestParam("payload") String payload) {
        return slackInteractiveService.handleInteractive(payload);
    }
}