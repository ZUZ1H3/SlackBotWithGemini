package com.zuzihe.slackbot.slack.bolt.infra;

import com.slack.api.Slack;
import com.slack.api.methods.AsyncMethodsClient;
import com.slack.api.methods.request.assistant.threads.AssistantThreadsSetStatusRequest;
import com.slack.api.methods.request.chat.ChatMeMessageRequest;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.chat.ChatUpdateRequest;
import com.slack.api.methods.request.conversations.ConversationsOpenRequest;
import com.slack.api.methods.request.views.ViewsPublishRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;
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
import java.util.concurrent.CompletableFuture;

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
                                .accessory(button(b -> b.text(plainText("채팅")).value("open_docbot_apispec").actionId("open_docbot_apispec")))
                        ),
                section(s -> s
                        .text(markdownText("*영업지원 문서봇*\n최근 대화한 날짜 · *2025-08-31 09:15*"))
                        .accessory(button(b -> b.text(plainText("채팅")).value("open_docbot_sales").actionId("open_docbot_sales")))
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

    /// 버튼 클릭 후 대화 생성
    public void openConversationAndSendWelcomeMessageWithChatRoomId(String userId, String chatRoomId) {
        // 1. DM 채널 열기
        var openRequest = ConversationsOpenRequest.builder()
                .users(List.of(userId))
                .build();

        clientAsync().conversationsOpen(openRequest)
                .thenAccept(response -> {
                    if (response.isOk()) {
                        String channelId = response.getChannel().getId();
                        log.info("[Bolt] DM 채널 열기 성공 - ChannelId: {}", channelId);

                        // 2. 환영 메시지 전송
                        sendNewChatWelcomeMessage(channelId, chatRoomId);
                    } else {
                        log.error("[Bolt] DM 채널 열기 실패: {}", response.getError());
                    }
                })
                .exceptionally(e -> {
                    log.error("[Bolt] DM 채널 열기 중 오류", e);
                    return null;
                });
    }

    private void sendNewChatWelcomeMessage(String channelId, String chatRoomId) {
        List<LayoutBlock> blocks = asBlocks(
                section(s -> s.text(markdownText("🎉 *새로운 채팅을 시작합니다!*"))),
                divider(),
                section(s->s.text(markdownText("현재 진행중인 채팅방: "+ chatRoomId))),
                section(s -> s.text(markdownText("안녕하세요! 저는 aichatter 봇입니다.\n무엇을 도와드릴까요?"))),
                section(s -> s.text(markdownText("*💡 예시 질문들:*\n• 오늘 날씨 어때?\n• 프로그래밍 질문하기\n• 창작 도움 요청하기")))
        );

        var req = ChatPostMessageRequest.builder()
                .channel(channelId)
                .text("새로운 채팅을 시작합니다!")
                .blocks(blocks)
                .build();

        clientAsync().chatPostMessage(req)
                .thenAccept(resp -> {
                    if (!resp.isOk()) {
                        log.error("[Bolt] 새 채팅 환영 메시지 전송 실패: {}", resp.getError());
                    } else {
                        log.info("[Bolt] 새 채팅 환영 메시지 전송 성공 - ChannelId: {}", channelId);
                    }
                })
                .exceptionally(e -> {
                    log.error("[Bolt] 새 채팅 환영 메시지 전송 중 오류", e);
                    return null;
                });
    }

    public void setThinkingStatus(String channel, String threadTs) {
        String thinkingText = "가(이) 답변을 생성하고 있습니다 \"\uD83D\uDCAD\"";

        AssistantThreadsSetStatusRequest req = AssistantThreadsSetStatusRequest.builder()
                .channelId(channel)
                .threadTs(threadTs)
                .status(thinkingText)
                .build();

        clientAsync().assistantThreadsSetStatus(req)
                .thenAccept(resp -> {
                    if (resp.isOk()) {
                        log.info("[Bolt] aichatter 스켈레톤 UI 전송 성공 - Channel: {}", channel);
                    } else {
                        log.error("[Bolt] aichatter 스켈레톤 UI 전송 성공: {}", resp.getError());
                    }
                })
                .exceptionally(e -> {
                    log.error("[Bolt] 답변 생성 중 메시지 전송 실패: {}", e);
                    return null;
                });

    }


    /**
     * 답변 생성 중임을 알리는 메서드는 setThinkingStatus() 사용하세요
     */
   @Deprecated(forRemoval = true)
    public CompletableFuture<String> sendThinkingMessage(String channel, String threadTs) {
        String thinkingText = "가(이) 답변을 생성하고 있습니다 \"\uD83D\uDCAD\"";

        var req = ChatPostMessageRequest.builder()
                .channel(channel)
                .threadTs(threadTs)
                .text(thinkingText)
                .build();

        return clientAsync().chatPostMessage(req)
                .thenApply(resp -> {
                    if (resp.isOk()) {
                        log.info("[Bolt] aichatter 스켈레톤 UI 전송 성공 - Channel: {}, Ts: {}", channel, resp.getTs());
                        return resp.getTs(); // 메시지의 타임스탬프 반환
                    } else {
                        log.error("[Bolt] aichatter 스켈레톤 UI 전송 성공: {}", resp.getError());
                        return null;
                    }
                })
                .exceptionally(e -> {
                    log.error("[Bolt] 답변 생성 중 메시지 전송 실패: {}", e);
                    return null;
                });
    }

    // 기존 메시지를 실제 답변으로 업데이트
    public void updateMessageWithResponse(String channel, String messageTs, String actualResponse) {
        var req = ChatUpdateRequest.builder()
                .channel(channel)
                .ts(messageTs)
                .text(actualResponse)
                .build();

        clientAsync().chatUpdate(req)
                .thenAccept(response -> {
                    if (response.isOk()) {
                        log.info("[Bolt] 메시지 업데이트 성공 - Channel: {}, Ts: {}", channel, messageTs);
                    } else {
                        log.error("[Bolt] 메시지 업데이트 실패: {}", response.getError());
                    }
                })
                .exceptionally(e -> {
                    log.error("[Bolt] 메시지 업데이트 중 오류", e);
                    return null;
                });
    }

}
