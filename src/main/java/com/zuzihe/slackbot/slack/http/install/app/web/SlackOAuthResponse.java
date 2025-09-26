package com.zuzihe.slackbot.slack.http.install.app.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)

public class SlackOAuthResponse {
    private boolean ok;
    private String access_token;    //BotUserOAuthToken과 동일
    private String bot_user_id;
    private boolean is_enterprise_install;
    private Team team;
    private String error;

    @Data
    public static class Team {
        private String id;
        private String name;
    }
}