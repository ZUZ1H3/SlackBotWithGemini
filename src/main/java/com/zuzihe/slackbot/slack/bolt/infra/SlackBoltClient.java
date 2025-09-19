package com.zuzihe.slackbot.slack.bolt.infra;

import com.slack.api.Slack;
import com.slack.api.methods.AsyncMethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.view.View;
import com.slack.api.model.view.Views;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackBoltClient {

    @Value("${slack.botToken}")
    private String botToken;

    private AsyncMethodsClient clientAsync() {
        return Slack.getInstance().methodsAsync(botToken);
    }

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

    public void publishAppHome(String userId, List<Map<String, Object>> blocks) {
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

    private boolean isAiChatterLinked() {
        // TODO 연동 구현
        return true;
    }
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
