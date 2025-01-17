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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Класс Chat представляет приложение для обмена сообщениями через P2P-сеть.
 */
public class ImprovedChat {
    private static Profile profile;
    private static String openDialogUserName;
    private EmbeddedIpfs embeddedIpfs;

    public ImprovedChat(Integer portNumber) throws ConnectionException {
        setupNode(portNumber);
        startChatLoop();
    }

    private static HttpProtocol.HttpRequestProcessor proxyHandler() {
        return (session, req, handler) -> {
            ByteBuf content = req.content();
            String messageContent = content.toString(StandardCharsets.UTF_8);
            String senderPeerId = req.headers().get("X-Sender-PeerId");

            Contact contact = profile.getContacts().stream()
                    .filter(c -> c.getOriginPeerId().equals(senderPeerId))
                    .findFirst()
                    .orElseGet(() -> {
                        // Создаем новый контакт, если он не найден
                        Contact newContact = new Contact(senderPeerId, "New Contact", senderPeerId,
                                PeerId.fromBase58(senderPeerId), Multihash.fromBase58(senderPeerId), new Dialog(new ArrayList<>()));
                        profile.getContacts().add(newContact);
                        return newContact;
                    });

            LocalDateTime receiveTime = LocalDateTime.now();
            Message message = new Message(messageContent, receiveTime);

            if (contact.getUsername().equals(openDialogUserName)) {
                System.out.println(receiveTime + ": " + messageContent);
                message.setNew(false);
            } else {
                message.setNew(true);
            }

            contact.getDialog().getMessages().add(message);
            handler.accept(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
        };
    }

    private void setupNode(Integer portNumber) throws ConnectionException {
        System.out.println("Starting Chat version: " + Version.parse("0.0.1"));

//        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip4/127.0.0.1/tcp/" + portNumber));
        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip6/::/tcp/" + portNumber)); // Адрес для связи
        List<MultiAddress> bootstrapNodes = Config.defaultBootstrapNodes;

        HostBuilder builder = new HostBuilder().generateIdentity();
        PrivKey privKey = builder.getPrivateKey();
        PeerId peerId = builder.getPeerId();

        System.out.println("My PeerId: " + peerId.toBase58());

        IdentitySection identity = new IdentitySection(privKey.bytes(), peerId);
        Blockstore blockStore = new RamBlockstore();
        RecordStore recordStore = new RamRecordStore();
        BlockRequestAuthoriser authoriser = (conn, peer, addr) -> CompletableFuture.completedFuture(true);

        embeddedIpfs = EmbeddedIpfs.build(recordStore, blockStore, false, swarmAddresses, bootstrapNodes,
                identity, authoriser, Optional.of(Chat.proxyHandler()));
        embeddedIpfs.start();

        setProfileInfo(peerId);
    }

    private void setProfileInfo(PeerId peerId) {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = in.nextLine().trim();

        System.out.print("Enter your info: ");
        String info = in.nextLine().trim();

        profile = new Profile(username, info, peerId.toBase58(), new ArrayList<>());
    }

    private void startChatLoop() {
        Scanner in = new Scanner(System.in);
        boolean isRunning = true;

        while (isRunning) {
            System.out.println("Enter command (1 - Add Contact, 2 - List Contacts, 3 - Open Chat, 4 - Exit):");
            String input = in.nextLine().trim();

            try {
                switch (Integer.parseInt(input.split(" ")[0])) {
                    case 1 -> addContact(in);
                    case 2 -> listContacts();
                    case 3 -> openChat(input.split(" ")[1]);
                    case 4 -> {
                        stop();
                        isRunning = false;
                    }
                    default -> System.out.println("Invalid command.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void addContact(Scanner in) {
        System.out.print("Enter contact username: ");
        String username = in.nextLine().trim();

        System.out.print("Enter contact PeerId: ");
        String peerIdStr = in.nextLine().trim();
        if (peerIdStr.isEmpty()) {
            throw new IllegalArgumentException("PeerId cannot be empty.");
        }

        Multihash nodeId = Multihash.fromBase58(peerIdStr);
        PeerId peerId = PeerId.fromBase58(peerIdStr);

        Contact contact = new Contact(username, "Added Contact", peerIdStr, peerId, nodeId, new Dialog(new ArrayList<>()));
        profile.getContacts().add(contact);
        System.out.println("Contact added successfully.");
    }

    private void listContacts() {
        System.out.println("Contacts:");
        profile.getContacts().forEach(contact -> System.out.println(contact.getUsername()));
    }

    private void openChat(String username) {
        profile.getContacts().stream()
                .filter(contact -> contact.getUsername().equals(username))
                .findFirst()
                .ifPresentOrElse(contact -> {
                    try {
                        Multiaddr[] addresses = EmbeddedIpfs.getAddresses(embeddedIpfs.node, embeddedIpfs.dht, contact.getTargetNodeId());
                        runChat(embeddedIpfs.node, embeddedIpfs.p2pHttp.get(), contact, addresses);
                    } catch (Exception e) {
                        System.out.println("Failed to open chat: " + e.getMessage());
                    }
                }, () -> System.out.println("Contact not found."));
    }

    public void runChat(Host node, HttpProtocol.Binding p2pHttpBinding, Contact contact, Multiaddr[] addressesToDial) {
        System.out.println("Chat with " + contact.getUsername() + " opened.");
        openDialogUserName = contact.getUsername();

        Scanner in = new Scanner(System.in);
        boolean isChatting = true;

        while (isChatting) {
            System.out.print("> ");
            String input = in.nextLine().trim();

            if (input.equalsIgnoreCase(">back")) {
                isChatting = false;
                break;
            }

            sendMessage(node, p2pHttpBinding, contact, addressesToDial, input);
        }
    }

    private void sendMessage(Host node, HttpProtocol.Binding binding, Contact contact, Multiaddr[] addresses, String message) {
        try {
            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/", Unpooled.wrappedBuffer(payload));
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, payload.length);
            request.headers().set("X-Sender-PeerId", node.getPeerId().toBase58());

            HttpProtocol.HttpController controller = binding.dial(node, contact.getTargetPeerId(), addresses).getController().join();
            controller.send(request).join().release();
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    public void stop() {
        if (embeddedIpfs != null) {
            try {
                embeddedIpfs.stop();
                System.out.println("Node stopped successfully.");
            } catch (Exception e) {
                System.err.println("Failed to stop node: " + e.getMessage());
            }
        }
    }
}
