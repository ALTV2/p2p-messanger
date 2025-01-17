package com.tveritin.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private final String payload;
    private final LocalDateTime localDateTime;
    private boolean isNew = false;
}
