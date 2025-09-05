package com.zuzihe.slackbot.controller;

import com.zuzihe.slackbot.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/*
user가 Slack 에서 /aichatter 질문 내용을 입력할 것이다
ex) /aichatter 오늘 점심메뉴추천 기기염
Slack이 8080 서버로 POST 요청 보냄

Content-Type: application/x-www-form-urlencoded

파라미터로 아래와 같은 정보 전달됨:
token=...
team_id=T123...
user_id=U456...
command=/hey
text=오늘 점심메뉴추천 기기염
response_url=https://hooks.slack.com/...
그러면 SlackController에서 해당 요청을 처리함
*/

@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

    private final SlackService slackService;

    @PostMapping(value = "/commands", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleCommand(@RequestParam Map<String, String> params) {
        String userId = params.get("user_id");
        String channelId = params.get("channel_id");
        String question = params.get("text");

        // 비동기로 처리 (3초 안에 OK만 보내기)
        slackService.askAndSendToSlack(channelId, question);

        return ResponseEntity.ok("질문을 받았습니다! 잠시만요…");
    }
}