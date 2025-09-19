package com.zuzihe.slackbot.link.account.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // JWT 발급
    public String issue(String slackUserId, String teamId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(Map.of(
                        "slack_user_id", slackUserId,
                        "team_id", teamId
                ))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 5 * 60 * 1000)) // TTL 5분
                .signWith(key)
                .compact();
    }

    // JWT 검증
    public Map<String, Object> verify(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
