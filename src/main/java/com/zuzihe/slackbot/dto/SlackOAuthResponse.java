package com.zuzihe.slackbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 👈 이거 추가!

public class SlackOAuthResponse {
    private boolean ok;
    private String access_token;
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