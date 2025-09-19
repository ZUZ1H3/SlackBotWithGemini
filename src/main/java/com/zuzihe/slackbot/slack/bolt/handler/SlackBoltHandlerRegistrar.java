package com.zuzihe.slackbot.slack.bolt.handler;

import com.slack.api.bolt.App;

public interface SlackBoltHandlerRegistrar {
    void register(App app);
}

