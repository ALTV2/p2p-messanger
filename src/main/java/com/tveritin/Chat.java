package com.tveritin;

import com.tveritin.domain.Contact;
import com.tveritin.domain.Dialog;
import com.tveritin.domain.Message;
import com.tveritin.domain.Profile;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.multiformats.Multiaddr;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
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
import org.peergos.util.Version;

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
    static private Profile profile;
    private EmbeddedIpfs embeddedIpfs;
    private Integer portNumber;
    static private String openDialogUserName;

    /**
     * Этот метод создает обработчик HTTP-запросов, который принимает сообщение и возвращает ответ "OK".
     * @return обработчик HTTP-запросов.
     */
    static HttpProtocol.HttpRequestProcessor proxyHandler() {
        return (s, req, h) -> {
            ByteBuf content = req.content();
            String output = content.getCharSequence(0, content.readableBytes(), Charset.defaultCharset()).toString();
            String senderPeerId = req.headers().get("X-Sender-PeerId");

            var optContact = profile.getContacts().stream().filter(contact -> contact.getOriginPeerId().equals(senderPeerId))
                    .findFirst(); // если option is empty нужно создавать новый контакт
            Contact contact;
            if (optContact.isPresent()) {
                contact = optContact.get();

                var receiveTime = LocalDateTime.now();
                var message = new Message(output, receiveTime);

                if (contact.getUsername().equals(openDialogUserName)) {
                    System.out.println(receiveTime + ": " + output);
                    message.setNew(false);
                } else message.setNew(true);

                contact.getDialog().getMessages().add(message);
            } else {
                System.out.println("Unknow message from peerId: " + senderPeerId + ", please add to contact or block.");
            }

            FullHttpResponse replyOk = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer(0));
            replyOk.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            h.accept(replyOk.retain());
        };
    }

    /**
     * Конструктор класса Chat создает локальный узел p2p-сети и запускает процесс общения.
     * @throws ConnectionException в случае возникновения ошибки при установлении соединения.
     */
    public Chat(Integer portNumber) throws ConnectionException {
        this.portNumber = portNumber;
        RecordStore recordStore = new RamRecordStore(); // Хранилище записей в оперативной памяти
        Blockstore blockStore = new RamBlockstore(); // Блок-хранилище в оперативной памяти

        System.out.println("Starting Chat version: " + Version.parse("0.0.1"));
//        int portNumber = 10000 + new Random().nextInt(50000); // Случайный порт для соединения
        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip6/::/tcp/" + portNumber)); // Адрес для связи
//        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip4/127.0.0.1/tcp/" + portNumber)); // IPv4); // Адрес для связи

        List<MultiAddress> bootstrapNodes = new ArrayList<>(Config.defaultBootstrapNodes);
        bootstrapNodes.add(new MultiAddress("/ip6/::/tcp/10003/p2p/12D3KooWHyypwophXqg1vSvfYLPvVCEBYiPKKQcmkWfBAiBNpC81"));

        HostBuilder builder = new HostBuilder().generateIdentity(); // Генерация идентификации узла
        PrivKey privKey = builder.getPrivateKey(); // Приватный ключ узла
        PeerId peerId = builder.getPeerId(); // Идентификатор узла
        System.out.println("My PeerId:" + peerId.toBase58());

        IdentitySection identitySection = new IdentitySection(privKey.bytes(), peerId); // Секция идентификации
        BlockRequestAuthoriser authoriser = (c, p, a) -> CompletableFuture.completedFuture(true); // Авторизация запросов

        // Создание и запуск IPFS узла с помощью библиотеки EmbeddedIpfs
        embeddedIpfs = EmbeddedIpfs.build(recordStore, blockStore, false,
                swarmAddresses,
                bootstrapNodes,
                identitySection,
                authoriser, Optional.of(Chat.proxyHandler()));
        embeddedIpfs.start();

        Scanner in = new Scanner(System.in);

        setProfileInfo(in, peerId);

        // 1 - добавить контакт
        // 2 - показать список контактов
        // 3 + " " + username открыть диалог
        boolean workFlag = true;
        while (workFlag) {
            System.out.println("Enter command:");
            String command = in.nextLine().trim();
            if (command.startsWith("3")) {
                var usernameForOpeningChat = command.split(" ")[1];
                // Запуск чата с указанным узлом
                var contactForOpeningChat = profile.getContacts().stream().filter( contact -> contact.getUsername().equals(usernameForOpeningChat))
                                .findFirst();
                try {
                    Contact contact = contactForOpeningChat.get();
                    String peerIdStr = contact.getOriginPeerId();

                    Multihash targetNodeId = Multihash.fromBase58(peerIdStr);
                    PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());

                    runChat(embeddedIpfs.node, embeddedIpfs.p2pHttp.get(), contact, targetPeerId,
                            EmbeddedIpfs.getAddresses(embeddedIpfs.node, embeddedIpfs.dht, targetNodeId));
                } catch (Exception ex) {
                    System.out.println("catch exception try again: " + ex.getMessage());
                }
            } else if (Integer.parseInt(command) == 1) {
                System.out.println("Adding contact:");

                System.out.println("Enter username:");
                String contactUsername = in.nextLine().trim();

                System.out.println("Enter PeerId of other node:");
                String peerIdStr = in.nextLine().trim();
                if (peerIdStr.length() == 0) {
                    throw new IllegalArgumentException("Invalid PeerId");
                }
                Multihash targetNodeId = Multihash.fromBase58(peerIdStr);
                PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());
                var newContact = new Contact(contactUsername, "someInfo", peerIdStr, targetPeerId, targetNodeId, new Dialog(new ArrayList<>()));
                profile.getContacts().add(newContact);
                System.out.println("Adding contact is finished.");
            } else if (Integer.parseInt(command) == 2){
                profile.getContacts().forEach(contact -> {
                    System.out.println(contact.getUsername());
                });
            } else if (Integer.parseInt(command) == 4){
                workFlag = false;
                stop();
            }
        }
    }

    private void setProfileInfo(Scanner in, PeerId peerId) {
        System.out.println("Enter you username:");
        String username = in.nextLine().trim();

        System.out.println("Enter you info:");
        String info = in.nextLine().trim();

        profile = new Profile(username, info, peerId.toBase58(), new ArrayList<>());
    }

    /**
     * Запускает процесс обмена сообщениями с целью отправки текстовых сообщений через p2p-соединение.
     *
     * @param node            узел, через который отправляются сообщения.
     * @param p2pHttpBinding  биндинг для http-протокола.
     * @param contact    идентификатор целевого узла.
     * @param addressesToDial адреса для соединения с целевым узлом.
     */
    public void runChat(Host node, HttpProtocol.Binding p2pHttpBinding, Contact contact, PeerId targetPeerId, Multiaddr[] addressesToDial) throws InterruptedException {
        System.out.println("Opening chat with " + contact.getUsername());
        openDialogUserName = contact.getUsername();

//        Thread.sleep(2000);
        AtomicBoolean ifFirstNewMessage = new AtomicBoolean(true);
        contact.getDialog().getMessages().forEach(message -> {
            if (ifFirstNewMessage.get() && message.isNew()){
                ifFirstNewMessage.set(false);
                System.out.println("||============== New UnRead Message =============||");
                System.out.println("\\/===============================================\\/");
            }
            if (message.isNew()) {
                message.setNew(false);
            }
            if (message.isMine()) {
                System.out.println(message.getLocalDateTime() + ": " + message.getPayload());
            } else {
                System.out.println("\t\t\t\t\t\t" + message.getPayload() + ": " + message.getLocalDateTime());
            }
        });
        Scanner in = new Scanner(System.in);
        byte[] payload;
        boolean goBack = false;
        while (!goBack) {
            payload = in.nextLine().trim().getBytes();

            String strPayload = new String(payload, StandardCharsets. UTF_8);
            if (strPayload.equals(">back")) {
                goBack = true;
                continue;
            }

            byte[] msg = payload;

            FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/", Unpooled.copiedBuffer(msg));
            httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, msg.length);
            httpRequest.headers().set("X-Sender-PeerId", node.getPeerId().toBase58());

            HttpProtocol.HttpController proxier = p2pHttpBinding.dial(node, targetPeerId, addressesToDial).getController().join();
            proxier.send(httpRequest.retain()).join().release();

            var newMyMessage = new Message(strPayload, LocalDateTime.now());
            newMyMessage.setNew(false);
            newMyMessage.setMine(true);
            contact.getDialog().getMessages().add(newMyMessage);
        }
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
}