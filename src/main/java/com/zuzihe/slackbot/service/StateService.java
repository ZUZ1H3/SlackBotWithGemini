package com.zuzihe.slackbot.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StateService {
    private final Map<String, Long> cache = new ConcurrentHashMap<>(); // 데모
    private final long TTL_MS = 10 * 60 * 1000;

    public String issue() {
        byte[] r = new byte[32];
        new SecureRandom().nextBytes(r);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(r);
        cache.put(state, System.currentTimeMillis() + TTL_MS);
        return state;
    }

    public boolean verifyAndConsume(String state) {
        Long exp = cache.remove(state);
        return exp != null && exp >= System.currentTimeMillis();
    }
}
