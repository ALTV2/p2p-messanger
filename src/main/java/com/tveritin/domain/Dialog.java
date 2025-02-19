package com.tveritin.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Dialog {
    private final List<Message> messages;
}
