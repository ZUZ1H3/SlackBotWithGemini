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

/**
 *
 *             switch (eventType) {
 *                 case "app_home_opened" -> handleAppHomeOpened(event); //홈 탭 열었을 때 (홈 UI 초기화, 안내 메시지 표시 등).
 *                 case "assistant_thread_started" -> handleAssistantThread(event); //새 채팅방 열었을 때
 *                 case "app_mention" -> handleAppMention(event); //@봇이름 으로 멘션 호출 되었을 때
 *                 case "message" -> handleMessage(event); //DM이나 채널에서 메시지 보냈을 때
 *                 default -> log.warn("지원하지 않는 이벤트 타입: {}", eventType);
 *             }
 */