package com.zuzihe.slackbot.slack.bolt.handler;

import com.slack.api.bolt.App;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.zuzihe.slackbot.slack.bolt.SlackBoltService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppHomeOpenedHandlerRegistrar implements SlackBoltHandlerRegistrar {
    private final SlackBoltService slackBoltService;

    @Override
    public void register(App app) {
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            var ev = payload.getEvent();
            slackBoltService.handleAppHomeOpened(ev.getUser());
            return ctx.ack();
        });
    }
}

