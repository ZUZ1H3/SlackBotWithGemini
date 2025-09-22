package com.zuzihe.slackbot.slack.bolt.handler;

import com.slack.api.bolt.App;
import com.slack.api.model.event.AppMentionEvent;
import com.zuzihe.slackbot.slack.bolt.SlackBoltService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppMentionHandlerRegistrar implements SlackBoltHandlerRegistrar {
    private final SlackBoltService slackBoltService;

    @Override
    public void register(App app) {
        app.event(AppMentionEvent.class, (payload, ctx) -> {
            var ev = payload.getEvent();
            if (ev.getText() != null && !ev.getText().isBlank()) {
                slackBoltService.handleAppMention(ev.getChannel(), ev.getText(), ev.getTs());
            }
            return ctx.ack();
        });
    }
}

