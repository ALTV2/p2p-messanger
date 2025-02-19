package com.tveritin;

import com.tveritin.domain.Contact;
import com.tveritin.domain.Dialog;
import com.tveritin.domain.Message;
import com.tveritin.domain.Profile;
import com.tveritin.service.ProfileService;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.PeerId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.peergos.EmbeddedIpfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatTest {

    public static final String PEER_ID = "peerId";
    @Mock
    private ProfileService profileService;

    @Mock
    private EmbeddedIpfs embeddedIpfs;

    private Chat chat;
    private final String testPeerId = "QmTestPeerId";
    private final String testUsername = "testUser";

//    @Test
//    void testAddNewContact() {
//        MockitoAnnotations.openMocks(this);
//
//        String input = "testUsername\n";
//        String contactUsername = "newContact";
//        String peerIdStr = PEER_ID;
//
//        System.setIn(new ByteArrayInputStream(input.getBytes()));
//
//        input = "1\n";
//        System.setIn(new ByteArrayInputStream(input.getBytes()));
//
//        input = contactUsername + "\n";
//        System.setIn(new ByteArrayInputStream(input.getBytes()));
//
//        input = peerIdStr + "\n";
//        System.setIn(new ByteArrayInputStream(input.getBytes()));
//
//        chat = new Chat(10000);
//        Chat.profileService = profileService;
//        chat.embeddedIpfs = embeddedIpfs;
//
//        verify(profileService).addNewContact(contactUsername, peerIdStr);
//    }
//
//    @Test
//    void testFillProfileInfo() {
//        String username = "testUser";
//        Profile profile = new Profile(username, "info", testPeerId, new ArrayList<>());
//
//        when(profileService.getUsername()).thenReturn(username);
//
//        chat.fillProfileInfo(new Scanner(username + "\n"), PeerId.fromBase58(testPeerId));
//
//        verify(profileService).getUsername();
//        assertEquals(username, profile.getUsername());
//    }
//
//    @Test
//    void testShowContacts() {
//        Multihash targetNodeId = Multihash.fromBase58(testPeerId);
//        PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());
//
//        var newContact = new Contact(
//                testUsername,
//                "Info",
//                testPeerId,
//                targetPeerId,
//                targetNodeId,
//                new Dialog(new ArrayList<>())
//        );
//
//        List<Contact> contacts = List.of(newContact);
//        when(profileService.getContacts()).thenReturn(contacts);
//
//        Chat.showContacts();
//
//        verify(profileService).getContacts();
//    }
//
//    @Test
//    void testOpenChatWithUser() {
//        String username = "testUser";
//        Multihash targetNodeId = Multihash.fromBase58(testPeerId);
//        PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());
//
//        var contact = new Contact(
//                username,
//                "Info",
//                testPeerId,
//                targetPeerId,
//                targetNodeId,
//                new Dialog(new ArrayList<>())
//        );
//        when(profileService.findContactByUsername(username)).thenReturn(Optional.of(contact));
//
//        chat.openChatWithUser("3 " + username);
//
//        verify(profileService).findContactByUsername(username);
//        // Here, you might want to check if runChat was called, but since it's private, you'd have to use reflection or make it public for testing.
//    }
//
//    @Test
//    void testStop() throws Exception {
//        doNothing().when(embeddedIpfs).stop();
//
//        chat.stop();
//
//        verify(embeddedIpfs).stop();
//    }
//
//    @Test
//    void testShowAlreadyReceivedMessages() {
//        Multihash targetNodeId = Multihash.fromBase58(testPeerId);
//        PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());
//
//        var contact = new Contact(
//                testUsername,
//                "Info",
//                testPeerId,
//                targetPeerId,
//                targetNodeId,
//                new Dialog(new ArrayList<>())
//        );
//
//        Message message = new Message("Test Message", LocalDateTime.now());
//        message.setNew(true);
//        contact.getDialog().getMessages().add(message);
//
//        Chat.showAlreadyReciveMessages(contact);
//
//        assertFalse(message.isNew(), "Message should be marked as read");
//    }
}
