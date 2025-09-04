package com.zuzihe.slackbot.controller;

import com.zuzihe.slackbot.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
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
    public Mono<ResponseEntity<String>> handleCommand(@RequestParam Map<String, String> params) {
        String question = params.get("text");

        return slackService.ask(question)
                .map(answer -> {
                    String responseJson = "%s".formatted(answer);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseJson);
                });
    }
}