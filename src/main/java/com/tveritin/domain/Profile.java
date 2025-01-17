package com.tveritin.domain;

import lombok.Data;

import java.util.List;

@Data
public class Profile {
    private final String username;
    private final String info;
    private final String selfPeerId;
    private final List<Contact> contacts;
}
