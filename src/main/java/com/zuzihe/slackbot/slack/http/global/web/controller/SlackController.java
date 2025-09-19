package com.zuzihe.slackbot.slack.http.global.web.controller;

//import com.zuzihe.slackbot.service.SlackCommandService;
import com.zuzihe.slackbot.slack.http.event.SlackEventRouter;
import com.zuzihe.slackbot.interacivity.SlackInteractiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/slack")
@Slf4j
@RequiredArgsConstructor
public class SlackController {

    private final SlackEventRouter slackEventRouter;
    private final SlackInteractiveService slackInteractiveService;

    /* Slack Slash Command 처리
    @PostMapping(value = "/commands", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleCommand(@RequestParam Map<String, String> params) {
        return slackCommandService.handleCommand(params);
    }
    */

    // [LEGACY] Slack Event API 처리 (Bolt로 대체됨)
    @PostMapping("/events-legacy")
    public ResponseEntity<String> handleEvent(@RequestBody Map<String, Object> payload) {
        log.info("[gno] event paylaod: {}", payload.toString());
        return slackEventRouter.routeEvent(payload);
    }

    // Slack Interactive Component 처리
    @PostMapping(value = "/interactive", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleInteractive(@RequestParam("payload") String payload) {
        return slackInteractiveService.handleInteractive(payload);
    }
}
