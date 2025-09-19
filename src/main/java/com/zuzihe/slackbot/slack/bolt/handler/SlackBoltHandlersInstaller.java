package com.zuzihe.slackbot.slack.bolt.handler;

import com.slack.api.bolt.App;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SlackBoltHandlersInstaller {
    private final App app;
    private final List<SlackBoltHandlerRegistrar> registrars;

    @PostConstruct
    public void installHandlers() {
        for (SlackBoltHandlerRegistrar r : registrars) {
            r.register(app);
        }
    }
}

