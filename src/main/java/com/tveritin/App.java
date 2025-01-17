package com.tveritin;

import org.peergos.net.ConnectionException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args) throws ConnectionException {
        System.out.println("Start");
        Chat chat = new Chat(Integer.parseInt(args[0]));
        Runtime.getRuntime().addShutdownHook(new Thread(chat::stop));
    }

    //запускам вторую потом первую, получилось сначала словить коннект из 2 в 1
    //Доделать историю переписки, очистить код
    //написать тесты

//    public static void main(String[] args) throws ConnectionException {
//        System.out.println("Start");
//        ImprovedChat chat = new ImprovedChat(Integer.parseInt(args[0]));
//        Runtime.getRuntime().addShutdownHook(new Thread(chat::stop));
//    }

//    public static void main(String[] args) throws ConnectionException {
//        System.out.println("Start");
//        new DefoultChat(Integer.parseInt(args[0]));
//    }
}
