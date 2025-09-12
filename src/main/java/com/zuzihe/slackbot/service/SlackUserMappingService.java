package com.zuzihe.slackbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SlackUserMappingService {



    private final Map<String, String> store = new HashMap<>();

    // 매핑 저장
    public void saveMapping(String teamId, String slackUserId, String aichatterUserId) {
        String key = teamId + ":" + slackUserId;
        store.put(key, aichatterUserId);
    }

    // 매핑 조회
    public String findAichatterUser(String teamId, String slackUserId) {
        String key = teamId + ":" + slackUserId;
        return store.get(key);
    }
}
