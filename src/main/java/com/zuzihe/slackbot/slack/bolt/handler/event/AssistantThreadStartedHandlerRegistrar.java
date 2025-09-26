package com.zuzihe.slackbot.slack.bolt.handler.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.App;
import com.slack.api.model.event.AssistantThreadStartedEvent;
import com.zuzihe.slackbot.slack.bolt.SlackBoltService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantThreadStartedHandlerRegistrar implements SlackBoltHandlerRegistrar {
    private final SlackBoltService slackBoltService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void register(App app) {
        app.event(AssistantThreadStartedEvent.class, (payload, ctx) -> {
            AssistantThreadStartedEvent event = payload.getEvent();
            String channelId = event.getAssistantThread().getChannelId();
            String threadTs = event.getAssistantThread().getThreadTs();
            if(channelId!=null && threadTs!=null) {
                slackBoltService.handleAssistantThread(channelId, threadTs);
            }
            return ctx.ack();
        });
    }
}

