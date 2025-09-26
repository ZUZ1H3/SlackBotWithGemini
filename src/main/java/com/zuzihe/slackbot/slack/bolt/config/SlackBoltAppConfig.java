package com.zuzihe.slackbot.slack.bolt.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackBoltAppConfig {

    @Value("${slack.signing-secret:}")
    private String signingSecret;

    @Value("${slack.botToken:}")
    private String botToken;    // workspace에 종속적이므로 workspace 구분가능

    @Bean
    public App slackApp() {
        AppConfig config = AppConfig.builder()
                .signingSecret(signingSecret)
                .singleTeamBotToken(botToken)
                .build();

        // Only build and expose App; handlers are registered by separate registrars
        return new App(config);
    }
}
