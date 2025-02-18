package com.tveritin;

import org.peergos.net.ConnectionException;

import java.util.Random;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args) throws ConnectionException {
        Chat chat = new Chat(Integer.parseInt(args[0]));
        Runtime.getRuntime().addShutdownHook(new Thread(chat::stop));
    }

//    public static void main(String[] args) throws ConnectionException {
//        System.out.println("Start In progress Chat");
//        var randomPort = 10000 + new Random().nextInt(50000);
//        System.out.println("Port is " + randomPort);
//        InProgressChat chat = new InProgressChat(randomPort);
//    }

    //запускам вторую потом первую, получилось сначала словить коннект из 2 в 1
    //Доделать историю переписки, очистить код
    //написать тесты
    //добавление контакта, когда он написал первый

//    public static void main(String[] args) throws ConnectionException {
//        System.out.println("Start");
//        ImprovedChat chat = new ImprovedChat(Integer.parseInt(args[0]));
//        Runtime.getRuntime().addShutdownHook(new Thread(chat::stop));
//    }

//    public static void main(String[] args) throws ConnectionException, InterruptedException {
//        System.out.println("Start");
//        new DefoultChat();
//    }

    /// DefaultChat is worked - 18:13 (without VPN; generate port number into main class)

    // чат доработать и проработать
    // отрефакторить код
    // протестировать
    // написать тесты
}
