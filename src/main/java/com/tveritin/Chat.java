package com.tveritin;

import com.tveritin.domain.Contact;
import com.tveritin.domain.Message;
import com.tveritin.domain.Profile;
import com.tveritin.service.ProfileService;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.multiformats.Multiaddr;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.peergos.BlockRequestAuthoriser;
import org.peergos.EmbeddedIpfs;
import org.peergos.HostBuilder;
import org.peergos.blockstore.Blockstore;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.config.Config;
import org.peergos.config.IdentitySection;
import org.peergos.net.ConnectionException;
import org.peergos.protocol.dht.RamRecordStore;
import org.peergos.protocol.dht.RecordStore;
import org.peergos.protocol.http.HttpProtocol;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Класс Chat представляет приложение для обмена сообщениями через P2P-сеть.
 * Использует библиотеку libp2p и HTTP-протокол для общения.
 */
public class Chat {
    public static final String X_SENDER_PEER_ID = "X-Sender-PeerId";
    private static final String X_SENDER_USERNAME = "X-Sender-Username";
    public static final String EMPTY = "Empty";
    public static final String BACK_COMMAND = ">back";
    public static final String TABS_FOR_RECIVE_MESSAGE = "\t\t\t\t\t\t";
    private static ProfileService profileService;
    private EmbeddedIpfs embeddedIpfs;
    static private String openDialogUserName;

    /**
     * Этот метод создает обработчик HTTP-запросов, который принимает сообщение и возвращает ответ "OK".
     *
     * @return обработчик HTTP-запросов.
     */
    static HttpProtocol.HttpRequestProcessor proxyHandler() {
        return (s, req, h) -> {
            ByteBuf content = req.content();
            String messageContent = content.getCharSequence(0, content.readableBytes(), Charset.defaultCharset()).toString();
            String senderPeerId = req.headers().get(X_SENDER_PEER_ID);
            String senderUsername = req.headers().get(X_SENDER_USERNAME);

            var optContact = profileService.findContactByPeerId(senderPeerId);

            if (optContact.isPresent()) {
                Contact contact = optContact.get();

                var receiveTime = LocalDateTime.now();
                var message = new Message(messageContent, receiveTime);

                if (contact.getUsername().equals(openDialogUserName)) {
                    printRecivedMessage(message);
                    message.setNew(false);
                }

                profileService.addMessageToDialog(contact, message);
            } else {
                profileService.addNewContactWithMessage(senderUsername, senderPeerId, messageContent);
                //todo добавить blackList + коллизии с одинаковыми username
            }

            FullHttpResponse replyOk = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer(0)
            );

            replyOk.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            h.accept(replyOk.retain());
        };
    }

    /**
     * Конструктор класса Chat создает локальный узел p2p-сети и запускает процесс общения.
     */
    public Chat(Integer portNumber) {
        RecordStore recordStore = new RamRecordStore();
        Blockstore blockStore = new RamBlockstore();

        System.out.println("Starting chat node on port - " + portNumber);
        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip6/::/tcp/" + portNumber));
        List<MultiAddress> bootstrapNodes = getBootStrapAddresses();

        HostBuilder builder = new HostBuilder().generateIdentity(); // Генерация идентификации узла
        PrivKey privKey = builder.getPrivateKey(); // Приватный ключ узла
        PeerId peerId = builder.getPeerId(); // Идентификатор узла
        System.out.println("My Chat - PeerId:" + peerId.toBase58());

        IdentitySection identitySection = new IdentitySection(privKey.bytes(), peerId); // Секция идентификации
        BlockRequestAuthoriser authoriser = (c, p, a) -> CompletableFuture.completedFuture(true); // Авторизация запросов

        // Создание и запуск IPFS узла с помощью библиотеки EmbeddedIpfs
        embeddedIpfs = EmbeddedIpfs.build(recordStore, blockStore, false,
                swarmAddresses,
                bootstrapNodes,
                identitySection,
                authoriser,
                Optional.of(Chat.proxyHandler())
        );

        embeddedIpfs.start();

        Scanner in = new Scanner(System.in);

        fillProfileInfo(in, peerId); // тут запрашиваем информацию о владельце ноды

        boolean workFlag = true;
        // '1'              - добавить контакт
        // '2'              - показать список контактов
        // '3 <username>'   - открыть диалог
        // '4'              - остановка работы мессенджера
        while (workFlag) {
            System.out.println("Enter command:");
            String command = in.nextLine().trim();

            if (command.startsWith("3")) {  // Запуск чата с указанным узлом
                openChatWithUser(command);
            } else if (Integer.parseInt(command) == 1) {
                addNewContact(in);
            } else if (Integer.parseInt(command) == 2) {
                showContacts();
            } else if (Integer.parseInt(command) == 4) {
                workFlag = false;
                stop();
            }
        }
    }

    private static void showContacts() {
        profileService.getContacts().forEach(
                contact -> {
                    System.out.println("    - " + contact.getUsername());
                }
        );
    }

    /**
     * Запускает процесс обмена сообщениями с целью отправки текстовых сообщений через p2p-соединение.
     *
     * @param node            узел, через который отправляются сообщения.
     * @param p2pHttpBinding  биндинг для http-протокола.
     * @param contact         идентификатор целевого узла.
     * @param addressesToDial адреса для соединения с целевым узлом.
     */
    public void runChat(Host node, HttpProtocol.Binding p2pHttpBinding, Contact contact, PeerId targetPeerId, Multiaddr[] addressesToDial) throws InterruptedException {
        System.out.println("<<< Opening chat with " + contact.getUsername() + " >>>");
        openDialogUserName = contact.getUsername();

        showAlreadyReciveMessages(contact);
        realTimeChatting(node, p2pHttpBinding, contact, targetPeerId, addressesToDial);

        openDialogUserName = EMPTY;
    }

    public void stop() {
        if (embeddedIpfs != null) {
            try {
                embeddedIpfs.stop();
                System.out.println("Node stopped and resources released.");
            } catch (Exception e) {
                System.err.println("Error while stopping node: " + e.getMessage());
            }
        }
    }

    private void fillProfileInfo(Scanner in, PeerId peerId) {
        System.out.println("Enter you username:");
        String username = in.nextLine().trim();

        var profile = new Profile(username, "info", peerId.toBase58(), new ArrayList<>());
        profileService = new ProfileService(profile);
    }

    @NotNull
    private static List<MultiAddress> getBootStrapAddresses() {
        List<MultiAddress> bootstrapNodes = new ArrayList<>(Config.defaultBootstrapNodes);
        bootstrapNodes.add(new MultiAddress("/ip6/::/tcp/10003/p2p/12D3KooWAkcb8qzZFe38gD3GjAuZQdhrBYpC9ZsTszNPohFzztoq"));
        return bootstrapNodes;
    }

    private void openChatWithUser(String command) {
        var usernameForOpeningChat = command.split(" ")[1];
        var contactOpt = profileService.findContactByUsername(usernameForOpeningChat);

        if (contactOpt.isPresent()) {
            try {
                Contact contact = contactOpt.get();

                Multihash targetNodeId = Multihash.fromBase58(contact.getOriginPeerId());
                PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());

                runChat(embeddedIpfs.node, embeddedIpfs.p2pHttp.get(), contact, targetPeerId,
                        EmbeddedIpfs.getAddresses(embeddedIpfs.node, embeddedIpfs.dht, targetNodeId));
            } catch (Exception ex) {
                System.out.println("Catch exception try again: " + ex.getMessage());
            }
        } else {
            System.out.println("Cant find contact with this username: " + usernameForOpeningChat);
        }
    }

    private static void addNewContact(Scanner in) {
        System.out.println("<<< Adding contact >>>");

        System.out.println("Enter username:");
        String contactUsername = in.nextLine().trim();

        System.out.println("Enter PeerId of other node:");
        String peerIdStr = in.nextLine().trim();

        if (peerIdStr.length() == 0) {
            throw new IllegalArgumentException("Invalid PeerId");
        }

        profileService.addNewContact(contactUsername, peerIdStr);
        System.out.println("Adding contact is finished.");
    }

    private static void showAlreadyReciveMessages(Contact contact) {
        AtomicBoolean isFirstNewMessage = new AtomicBoolean(true);
        contact.getDialog().getMessages().forEach(message -> {
            if (isFirstNewMessage.get() && message.isNew()) {
                isFirstNewMessage.set(false);
                showUnreadedMessageBanner();
            }

            if (message.isNew()) {
                message.setNew(false);
            }

            if (message.isMine()) {
                printMyMessage(message);
            } else {
                printRecivedMessage(message);
            }

        });
    }

    private static void printRecivedMessage(Message message) {
        System.out.println(TABS_FOR_RECIVE_MESSAGE + message.getPayload() + ": " + message.getLocalDateTime());
    }

    private static void printMyMessage(Message message) {
        System.out.println(message.getLocalDateTime() + ": " + message.getPayload());
    }

    private static void showUnreadedMessageBanner() {
        System.out.println("||============== New UnRead Message =============||");
        System.out.println("\\/===============================================\\/");
    }

    private static void realTimeChatting(Host node, HttpProtocol.Binding p2pHttpBinding, Contact contact, PeerId targetPeerId, Multiaddr[] addressesToDial) {
        Scanner in = new Scanner(System.in);
        byte[] payload;
        boolean goBack = false;
        while (!goBack) {
            payload = in.nextLine().trim().getBytes();

            String strPayload = new String(payload, StandardCharsets.UTF_8);
            if (strPayload.equals(BACK_COMMAND)) {
                goBack = true;
                continue;
            }

            byte[] msg = payload;

            FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/", Unpooled.copiedBuffer(msg));
            httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, msg.length);
            httpRequest.headers().set(X_SENDER_PEER_ID, node.getPeerId().toBase58());
            httpRequest.headers().set(X_SENDER_USERNAME, profileService.getUsername());

            HttpProtocol.HttpController proxier = p2pHttpBinding.dial(node, targetPeerId, addressesToDial).getController().join();
            proxier.send(httpRequest.retain()).join().release();

            var newMyMessage = new Message(strPayload, LocalDateTime.now());
            newMyMessage.setNew(false);
            newMyMessage.setMine(true);

            profileService.addMessageToDialog(contact, newMyMessage);
        }
    }
}