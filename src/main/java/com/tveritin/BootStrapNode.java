package com.tveritin;

import io.ipfs.multiaddr.MultiAddress;
import io.libp2p.core.Connection;
import io.libp2p.core.ConnectionHandler;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.PrivKey;
import org.jetbrains.annotations.NotNull;
import org.peergos.BlockRequestAuthoriser;
import org.peergos.EmbeddedIpfs;
import org.peergos.HostBuilder;
import org.peergos.blockstore.Blockstore;
import org.peergos.blockstore.RamBlockstore;
import org.peergos.config.Config;
import org.peergos.config.IdentitySection;
import org.peergos.protocol.dht.RamRecordStore;
import org.peergos.protocol.dht.RecordStore;
import org.peergos.util.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BootStrapNode {
    public static void main(String[] args) {
        int portNumber = 53026;
        RecordStore recordStore = new RamRecordStore(); // Хранилище записей в оперативной памяти
        Blockstore blockStore = new RamBlockstore(); // Блок-хранилище в оперативной памяти

        System.out.println("Starting Chat version: " + Version.parse("0.0.1"));
//        int portNumber = 10000 + new Random().nextInt(50000); // Случайный порт для соединения
        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip6/::/tcp/" + portNumber)); // Адрес для связи
//        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip4/127.0.0.1/tcp/" + portNumber)); // IPv4); // Адрес для связи

//        List<MultiAddress> bootstrapNodes = new ArrayList<>();
        List<MultiAddress> bootstrapNodes = new ArrayList<>(Config.defaultBootstrapNodes);


        HostBuilder builder = new HostBuilder().generateIdentity(); // Генерация идентификации узла
        PrivKey privKey = builder.getPrivateKey(); // Приватный ключ узла
        PeerId peerId = builder.getPeerId(); // Идентификатор узла
        System.out.println("My PeerId:" + peerId.toBase58());

        IdentitySection identitySection = new IdentitySection(privKey.bytes(), peerId); // Секция идентификации
        BlockRequestAuthoriser authoriser = (c, p, a) -> CompletableFuture.completedFuture(true); // Авторизация запросов

        // Создание и запуск IPFS узла с помощью библиотеки EmbeddedIpfs
        EmbeddedIpfs embeddedIpfs = EmbeddedIpfs.build(recordStore, blockStore, false,
                swarmAddresses,
                bootstrapNodes,
                identitySection,
                authoriser, Optional.of(Chat.proxyHandler()));

        // Добавляем слушателя событий для отслеживания подключений
        Host host = embeddedIpfs.node;
        host.addConnectionHandler(connection -> {
            System.out.println("!!!!" + connection.localAddress().getPeerId().toBase58());
            System.out.println("!!!!" + connection.remoteAddress().getPeerId().toBase58());
        });

        embeddedIpfs.start();
    }
}
