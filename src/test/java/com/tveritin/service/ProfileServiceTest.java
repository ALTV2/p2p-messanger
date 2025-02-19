package com.tveritin.service;

import com.tveritin.domain.*;
import io.libp2p.core.PeerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceTest {
    public static final String PEER_ID = "12D3KooWAkcb8qzZFe38gD3GjAuZQdhrBYpC9ZsTszNPohFzztoq";
    private ProfileService profileService;
    private Profile profile;

    @BeforeEach
    void setUp() {
        profile = new Profile("TestUser", "TestInfo", "12D3KooWAkcb8qzZFe38gD3GjAuZQdhrBYpC9ZsTszNPohFzztoW", new ArrayList<Contact>());
        profileService = new ProfileService(profile);
    }

    @Test
    void testAddNewContact() {
        profileService.addNewContact("Alice", PEER_ID);
        Optional<Contact> contact = profileService.findContactByPeerId(PEER_ID);

        assertTrue(contact.isPresent());
        assertEquals("Alice", contact.get().getUsername());
    }

    @Test
    void testFindContactByUsername() {
        profileService.addNewContact("Bob", PEER_ID);
        Optional<Contact> contact = profileService.findContactByUsername("Bob");
        assertTrue(contact.isPresent());
        assertEquals("Bob", contact.get().getUsername());
    }

    @Test
    void testAddMessageToDialog() {
        Contact contact = new Contact("Charlie", "info", PEER_ID,
                PeerId.random(), null, new Dialog(new ArrayList<>()));
        Message message = new Message("Hello!", LocalDateTime.now());
        profileService.addMessageToDialog(contact, message);
        assertEquals(1, contact.getDialog().getMessages().size());
        assertEquals("Hello!", contact.getDialog().getMessages().get(0).getPayload());
    }

    @Test
    void testAddNewContactWithMessage() {
        profileService.addNewContactWithMessage("Dave", PEER_ID, "Initial message");
        Optional<Contact> contact = profileService.findContactByPeerId(PEER_ID);
        assertTrue(contact.isPresent());
        assertEquals("Dave", contact.get().getUsername());
        assertEquals(1, contact.get().getDialog().getMessages().size());
        assertEquals("Initial message", contact.get().getDialog().getMessages().get(0).getPayload());
    }

    @Test
    void testGetContacts() {
        profileService.addNewContact("Alice", PEER_ID);
        profileService.addNewContact("Bob", "12D3KooWAkcb8qzZFe38gD3GjAuZQdhrBYpC9ZsTszNPohFzztoX");
        List<Contact> contacts = profileService.getContacts();
        assertEquals(2, contacts.size());
    }

    @Test
    void testGetUsername() {
        assertEquals("TestUser", profileService.getUsername());
    }
}
