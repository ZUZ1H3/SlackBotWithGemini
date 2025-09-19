package com.zuzihe.slackbot.slack.util;

import java.util.Map;

public class SlackBlockBuilder {

    ///**************************헬퍼메서드
    public static Map<String, Object> section(String markdownText) {
        return Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text", markdownText)
        );
    }

    // 구분선(divider)
    public static Map<String, Object> divider() {
        return Map.of("type", "divider");
    }

    // 버튼
    public static Map<String, Object> button(String text, String actionId) {
        return Map.of(
                "type", "button",
                "text", Map.of("type", "plain_text", "text", text),
                "action_id", actionId
        );
    }

    public static Map<String, Object> sectionWithButton(String markdownText, Map<String, Object> button) {
        return Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text", markdownText),
                "accessory", button
        );
    }

    public static Map<String, Object> urlButton(String text, String url) {
        return Map.of(
                "type", "button",
                "text", Map.of("type", "plain_text", "text", text),
                "url", url,
                "style", "primary"
        );
    }
}
