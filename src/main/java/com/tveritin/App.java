package com.tveritin;

import org.peergos.net.ConnectionException;

public class App {
    public static void main(String[] args) throws ConnectionException {
        Chat chat = new Chat(Integer.parseInt(args[0]));
        Runtime.getRuntime().addShutdownHook(new Thread(chat::stop));
    }
}
