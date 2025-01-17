package com.tveritin.domain;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Dialog {
    private final ArrayList<Message> messages;
}
