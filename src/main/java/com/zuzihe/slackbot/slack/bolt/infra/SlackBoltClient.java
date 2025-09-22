package com.zuzihe.slackbot.slack.bolt.infra;

import com.slack.api.Slack;
import com.slack.api.methods.AsyncMethodsClient;
import com.slack.api.methods.request.chat.ChatMeMessageRequest;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.Views;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackBoltClient {

    @Value("${slack.botToken}")
    private String botToken;

    private AsyncMethodsClient clientAsync() {
        return Slack.getInstance().methodsAsync(botToken);
    }

    // 스레드에 메시지 전송
    public void sendMessageWithThread(String channel, String text, String threadTs) {
        var req = ChatPostMessageRequest.builder()
                .channel(channel)
                .threadTs(threadTs)
                .text(text)
                .build();
        clientAsync().chatPostMessage(req)
                .thenAccept(resp -> {
                    if (!resp.isOk()) {
                        log.error("Bolt chat.postMessage failed: {}", resp.getError());
                    }
                })
                .exceptionally(e -> {
                    log.error("Bolt chat.postMessage error", e);
                    return null;
                });
    }

    public void publishAppHome(String userId) {
        View view = Views.view(v ->
                v.type("home")
                        .blocks(isAiChatterLinked() ? getLinkedBlocks() : getUnlinkedBlocks(userId))
        );

        var req = ViewsPublishRequest.builder()
                .userId(userId)
                .view(view)
                .build();
        clientAsync().viewsPublish(req)
                .thenAccept(resp -> {
                    if (!resp.isOk()) {
                        log.error("Bolt views.publish failed: {}", resp.getError());
                    }
                })
                .exceptionally(e -> {
                    log.error("Bolt views.publish error", e);
                    return null;
                });
    }

    public void sendWelcomeMessageWithButtons(String channelId, String threadTs) {
        List<LayoutBlock> blocks = asBlocks(
                section(s -> s.text(markdownText("안녕하세요 \n저는 aichatter with bolt 입니다"))),
                section(s -> s.text(markdownText("\n아래는 예시 프롬프트입니다."))),
                actions(a -> a.elements(asElements(
                        button(b -> b.text(plainText("button 예시 텍스트: aichatter란?"))
                                .value("latest_trends")
                                .actionId("latest_trends")
                        ),
                        button(b -> b.text(plainText("문서봇을 만드는 방법이란"))
                                .value("b2b_social_media")
                                .actionId("b2b_social_media")
                        )
                ))),
                section(s -> s.text(markdownText("아래는 문서봇 목록입니다"))),
                actions(a -> a.elements(asElements(
                        button(b -> b.text(plainText(p -> p.text("문서봇1")))
                                .value("customer_feedback")
                                .actionId("customer_feedback")
                        ),
                        button(b -> b.text(plainText(p -> p.text("문서봇2")))
                                .value("product_brainstorm")
                                .actionId("product_brainstorm")
                        )
                ))),
                input(i -> i
                        .blockId("block-id")
                        .label(plainText("문서봇 목록"))
                        .element(
                                staticSelect(s -> s
                                        .options(asOptions(
                                                option(o -> o.text(plainText("문서봇1"))),
                                                option(o -> o.text(plainText("문서봇2")))
                                        ))
                                        .placeholder(plainText("원하는 문서봇을 선택해주세요")))
                        ))
        );

        try {
            ChatPostMessageResponse resp = clientAsync().chatPostMessage(r -> r
                    .channel(channelId)
                    .threadTs(threadTs)
                    .text("환영 메시지")
                    .blocks(blocks)
            ).get();
            if (!resp.isOk()) {
                log.error("환영 메시지 전송 실패: {}", resp.getError());
            }
        } catch (Exception e) {
            log.error("환영 메시지 전송 중 오류", e);
        }
    }

    private boolean isAiChatterLinked() {
        // TODO 연동 구현
        return true;
    }
    // channel에 메시지 전송
    public void sendMessage(String channel, String text) {
        var req = ChatPostMessageRequest.builder()
                .channel(channel)
                .text(text)
                .build();
        clientAsync().chatPostMessage(req)
                .thenAccept(resp -> {
                    if (!resp.isOk()) {
                        log.error("Bolt chat.postMessage failed: {}", resp.getError());
                    }
                })
                .exceptionally(e -> {
                    log.error("Bolt chat.postMessage error", e);
                    return null;
                });
    }
    private List<LayoutBlock> getLinkedBlocks() {

        return asBlocks(
                section(s -> s.text(markdownText("👋 *안녕하세요, aichatter입니다.*"))),
                divider(),
                section(s -> s.text(markdownText("*나의 문서봇*"))),
                divider(),
                section(s -> s
                                .text(markdownText("*아이채터 정보봇*\n최근 대화한 날짜 · *1일 전*"))
                                .accessory(button(b -> b.text(plainText("채팅")).value("open_docbot_apispec")))
                        ),
                section(s -> s
                        .text(markdownText("*영업지원 문서봇*\n최근 대화한 날짜 · *2025-08-31 09:15*"))
                        .accessory(button(b -> b.text(plainText("채팅")).value("open_docbot_sales")))
                )
        );
    }
    private List<LayoutBlock> getUnlinkedBlocks(String userId) {
        String loginUrl = "http://localhost:8081/slack/sign-in?slack_user_id=" + userId + "&team_id=d1234";

        return asBlocks(
                section(s -> s.text(markdownText("*aichatter를 슬랙에서 사용하려면 먼저 계정을 연동해주세요.*"))),
                divider(),
                actions(a -> a.elements(asElements(
                        button(b -> b.text(plainText("🔗 aichatter 로그인하기"))
                                .url(loginUrl)
                                .value("login_btn"))
                )))
        );
    }

}
