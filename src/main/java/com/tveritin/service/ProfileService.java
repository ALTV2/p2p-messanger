package com.tveritin.service;

import com.tveritin.domain.Contact;
import com.tveritin.domain.Dialog;
import com.tveritin.domain.Message;
import com.tveritin.domain.Profile;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.PeerId;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProfileService {
    private final Profile profile;

    public Optional<Contact> findContactByPeerId(String peerId) {
        return profile.getContacts().stream()
                .filter(contact -> contact.getOriginPeerId().equals(peerId))
                .findFirst();
    }

    public void addMessageToDialog(Contact contact, Message message) {
        contact.getDialog().getMessages().add(message);
    }

    public void addNewContactWithMessage(String username, String peerId, String messagePayload) {
        Multihash targetNodeId = Multihash.fromBase58(peerId);
        PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());

        Message message = new Message(
                messagePayload,
                LocalDateTime.now()
        );

        List<Message> messageList = new ArrayList<>();
        messageList.add(message);

        Dialog dialog = new Dialog(messageList);

        profile.getContacts().add(
                new Contact(
                        username,
                        "info",
                        peerId,
                        targetPeerId,
                        targetNodeId,
                        dialog
                )
        );
    }

    public void addNewContact(String username, String peerIdStr) {
        Multihash targetNodeId = Multihash.fromBase58(peerIdStr);
        PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());

        var newContact = new Contact(
                username,
                "Info",
                peerIdStr,
                targetPeerId,
                targetNodeId,
                new Dialog(new ArrayList<>())
        );
        profile.getContacts().add(newContact);
    }

    public Optional<Contact> findContactByUsername(String username) {
        return profile.getContacts().stream()
                .filter(contact -> contact.getUsername().equals(username))
                .findFirst();
    }

    public List<Contact> getContacts() {
        return profile.getContacts();
    }

    public String getUsername() {
        return profile.getUsername();
    }
}
