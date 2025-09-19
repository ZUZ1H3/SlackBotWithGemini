package com.zuzihe.slackbot.link.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackSignInService {

    private final JwtService jwtService;
    private final SlackUserMappingService mappingService;

    public ResponseEntity<Void> startSignIn(String slackUserId, String teamId) {
        String token = jwtService.issue(slackUserId, teamId);
        URI redirect = URI.create("http://localhost:8080/api/login?token=" + token);
        return ResponseEntity.status(302).location(redirect).build();
    }

    public ResponseEntity<String> checkMapping(String slackUserId, String teamId) {
        String user = mappingService.findAichatterUser(teamId, slackUserId);
        return ResponseEntity.ok("매핑 조회 결과: " + user);
    }

    /// client == slack 이고 별도로 access token을 slack client가 저장해두고  aichatter-be한테 줄 수
    /// 없으니 aichatter 측에서 (access token, slack user id)를 같이 저장해둔다는 뜻인듯
    public ResponseEntity<String> completeSignIn(String token, String aichatterUserId) {
        Map<String, Object> claims = jwtService.verify(token);
        String slackUserId = (String) claims.get("slack_user_id");
        String teamId = (String) claims.get("team_id");

        mappingService.saveMapping(teamId, slackUserId, aichatterUserId);
        return ResponseEntity.ok("연동 성공: " + slackUserId + " → " + aichatterUserId);
    }
}