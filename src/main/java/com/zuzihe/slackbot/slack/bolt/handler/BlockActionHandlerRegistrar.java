package com.zuzihe.slackbot.slack.bolt.handler;

import com.slack.api.bolt.App;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.event.MessageEvent;
import com.zuzihe.slackbot.slack.bolt.SlackBoltService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockActionHandlerRegistrar implements SlackBoltHandlerRegistrar {
    private final SlackBoltService slackBoltService;

    @Override
    public void register(App app) {
        // 모든 블록 액션 로깅
        app.blockAction(Pattern.compile(".*") ,(req, ctx) -> {
            String actionId = req.getPayload().getActions().get(0).getActionId();
            String userId = req.getPayload().getUser().getId();

            log.info("[BlockAction] 모든 버튼 클릭 감지 - ActionId: {}, UserId: {}", actionId, userId);

            return ctx.ack();
        });

        // 특정 채팅 버튼 클릭 처리
        app.blockAction("open_docbot_apispec", (req, ctx) -> {
            String userId = req.getPayload().getUser().getId();
            log.info("[BlockAction] API 정보봇 채팅 버튼 클릭 - UserId: {}", userId);
            slackBoltService.openNewChatWithUser(userId, "roomA");
            return ctx.ack();
        });

        app.blockAction("open_docbot_sales", (req, ctx) -> {
            String userId = req.getPayload().getUser().getId();
            log.info("[BlockAction] 영업지원봇 채팅 버튼 클릭 - UserId: {}", userId);
            slackBoltService.openNewChatWithUser(userId, "roomB");
            return ctx.ack();
        });
    }
}