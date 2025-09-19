package com.zuzihe.slackbot.slack.bolt.handler;

import com.slack.api.bolt.App;
import com.slack.api.model.event.MessageEvent;
import com.zuzihe.slackbot.slack.bolt.SlackBoltService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectMessageHandlerRegistrar implements SlackBoltHandlerRegistrar {

    private final SlackBoltService slackBoltService;

    @Override
    public void register(App app) {
        app.event(MessageEvent.class, (payload, ctx) -> {
            MessageEvent ev = payload.getEvent();
            if ("im".equals(ev.getChannelType()) && ev.getBotId() == null && ev.getText() != null && !ev.getText().isBlank()) {
                slackBoltService.handleDirectMessage(ev);
            }
            return ctx.ack();
        });
    }
}
