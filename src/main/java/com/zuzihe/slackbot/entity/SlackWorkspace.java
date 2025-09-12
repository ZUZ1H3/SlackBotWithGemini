package com.zuzihe.slackbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;

import java.time.OffsetDateTime;

@Entity
@Getter
public class SlackWorkspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(name = "team_id")
    private String teamId; //T0123

    @NonNull
    @Column(name = "bot_token")
    private String botToken; //암호화

    private OffsetDateTime installed_at;


}
