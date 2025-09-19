package com.zuzihe.slackbot.slack;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public enum SlackEventType {
    MESSAGE("message"),
    APP_HOME_OPENED("app_home_opened"),
    APP_MENTION("app_mention"),
    ASSISTANT_THREAD_STARTED("assistant_thread_started"),
    ;
    private final String value;

    public static boolean isRegisteredEventType(String eventType) {
        return Arrays.stream(SlackEventType.values())
                .anyMatch(t -> t.value.equals(eventType));

    }
}
