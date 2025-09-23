package com.zuzihe.slackbot.slack.bolt.web;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackBoltServletConfig {

    @Bean
    public ServletRegistrationBean<SlackAppServlet> slackEventsServlet(App app) {
        // Map Bolt's servlet to the same path the app expects for Events API
        SlackAppServlet servlet = new SlackAppServlet(app);
        return new ServletRegistrationBean<>(servlet, "/api/slack/events");
    }

    @Bean
    public ServletRegistrationBean<SlackAppServlet> slackInteractivityServlet(App app) {
        SlackAppServlet servlet = new SlackAppServlet(app);
        return new ServletRegistrationBean<>(servlet, "/api/slack/interactive");
    }
}
