package com.zuzihe.slackbot.controller;

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
public class SlackController {

    @PostMapping(value = "/commands", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleCommand(@RequestParam Map<String, String> params) {
        System.out.println(" Slash Command 도착!");
        System.out.println("params = " + params);

        // Slack 응답 (에코용)
        String responseText = "AI에게 보낸 질문: " + params.get("text");

        // Slack에 응답 보낼 JSON
        String responseJson = """
        {
          "response_type": "ephemeral",
          "text": "%s"
        }
        """.formatted(responseText);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseJson);
    }
}

