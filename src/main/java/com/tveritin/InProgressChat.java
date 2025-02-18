package com.tveritin;

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
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class InProgressChat {
    private EmbeddedIpfs embeddedIpfs;

    private static HttpProtocol.HttpRequestProcessor proxyHandler() {
        return (s, req, h) -> {
            ByteBuf content = req.content();
            String output = content.getCharSequence(0, content.readableBytes(), Charset.defaultCharset()).toString();
            System.out.println("received msg:" + output);
            FullHttpResponse replyOk = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer(0));
            replyOk.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            h.accept(replyOk.retain());
        };
    }
    public InProgressChat(int portNumber) throws ConnectionException {
        RecordStore recordStore = new RamRecordStore();
        Blockstore blockStore = new RamBlockstore();

        System.out.println("Starting Chat version: " + Version.parse("0.0.1"));
        List<MultiAddress> swarmAddresses = List.of(new MultiAddress("/ip6/::/tcp/" + portNumber));
//        List<MultiAddress> bootstrapNodes = new ArrayList<>(Config.defaultBootstrapNodes);

        List<MultiAddress> bootstrapNodes = List.of(new MultiAddress("/dnsaddr/bootstrap.libp2p.io/p2p/QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN"));

        HostBuilder builder = new HostBuilder().generateIdentity();
        PrivKey privKey = builder.getPrivateKey();
        PeerId peerId = builder.getPeerId();
        System.out.println("My PeerId:" + peerId.toBase58());
        IdentitySection identitySection = new IdentitySection(privKey.bytes(), peerId);
        BlockRequestAuthoriser authoriser = (c, p, a) -> CompletableFuture.completedFuture(true);

        embeddedIpfs = EmbeddedIpfs.build(recordStore, blockStore, false,
                swarmAddresses,
                bootstrapNodes,
                identitySection,
                authoriser, Optional.of(Chat.proxyHandler()));
        embeddedIpfs.start();
        System.out.println("Enter PeerId of other node:");
        Scanner in = new Scanner(System.in);
        String peerIdStr = in.nextLine().trim();
        if (peerIdStr.length() == 0) {
            throw new IllegalArgumentException("Invalid PeerId");
        }
        Multihash targetNodeId = Multihash.fromBase58(peerIdStr);
        PeerId targetPeerId = PeerId.fromBase58(targetNodeId.toBase58());
        runChat(embeddedIpfs.node, embeddedIpfs.p2pHttp.get(), targetPeerId,
                EmbeddedIpfs.getAddresses(embeddedIpfs.node, embeddedIpfs.dht, targetNodeId));
    }
    private void runChat(Host node, HttpProtocol.Binding p2pHttpBinding, PeerId targetPeerId, Multiaddr[] addressesToDial) {
        System.out.println("Type message:");
        Scanner in = new Scanner(System.in);
        while (true) {
            byte[] msg = in.nextLine().trim().getBytes();
            FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/", Unpooled.copiedBuffer(msg));
            httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, msg.length);
            HttpProtocol.HttpController proxier = p2pHttpBinding.dial(node, targetPeerId, addressesToDial).getController().join();
            proxier.send(httpRequest.retain()).join().release();
        }
    }
}