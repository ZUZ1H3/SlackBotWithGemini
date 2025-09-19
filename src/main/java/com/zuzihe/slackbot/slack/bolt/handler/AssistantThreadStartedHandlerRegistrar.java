package com.zuzihe.slackbot.slack.bolt.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.App;
import com.slack.api.bolt.middleware.Middleware;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.AssistantThreadStartedEvent;
import com.zuzihe.slackbot.slack.bolt.SlackBoltService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantThreadStartedHandlerRegistrar implements SlackBoltHandlerRegistrar {
    private final SlackBoltService slackBoltService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void register(App app) {
/*
        // Use middleware to capture unknown event type 'assistant_thread_started'
        Middleware mw = (req, resp, chain) -> {
            try {
                String body = req.getRequestBodyAsString();
                Map<String, Object> root = objectMapper.readValue(body, new TypeReference<>() {
                });

                Map<String, Object> event = (Map<String, Object>) root.get("event");
                if (event != null && event.get("type").equals("assistant_thread_started")) {
                    Map<String, Object> assistantThread = (Map<String, Object>) event.get("assistant_thread");
                    if (assistantThread != null) {
                        String channelId = (String) assistantThread.get("channel_id");
                        String threadTs = (String) assistantThread.get("thread_ts");
                        if(channelId!=null && threadTs!=null) {
                            slackBoltService.handleAssistantThread(channelId, threadTs);
                        }
                    }
                }

            } catch (Exception e) {
                log.warn("assistant_thread_started parsing failed", e);
            }
            return chain.next(req);
        };

        app.use(mw);
    }
    */
        app.event(AssistantThreadStartedEvent.class, (paylaod, ctx) -> {
            AssistantThreadStartedEvent event = paylaod.getEvent();
            String channelId = event.getAssistantThread().getChannelId();
            String threadTs = event.getAssistantThread().getThreadTs();
            if(channelId!=null && threadTs!=null) {
                slackBoltService.handleAssistantThread(channelId, threadTs);
            }
            return ctx.ack();
        });
    }
}

