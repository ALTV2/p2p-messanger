//package com.tveritin.newimpl;
//
//import io.libp2p.core.*;
//import io.libp2p.core.multiformats.*;
//import io.libp2p.core.mux.*;
//import io.libp2p.core.streams.*;
//import io.libp2p.transport.tcp.TcpTransport;
//import io.libp2p.crypto.keys.*;
//import io.libp2p.etc.types.*;
//
//import java.util.concurrent.*;
//import java.util.function.*;
//
//public class P2PMessenger {
//    public static void main(String[] args) throws Exception {
//        Host host = createHost();
//        host.start().get();
//
//        System.out.println("Node started. Listening on: " + host.getAddresses());
//
//        host.getNetwork().listen(P2PChannelHandler.create());
//
//        if (args.length > 0) {
//            String targetPeer = args[0];
//            connectToPeer(host, targetPeer);
//        }
//    }
//
//    private static Host createHost() {
//        return new HostBuilder()
//                .transport(TcpTransport::new)
//                .secure(NoiseXXSecureChannel::new)
//                .mux(MplexStreamMuxer::new)
//                .listen(/ip4/127.0.0.1/tcp/4001)
//                .build();
//    }
//
//    private static void connectToPeer(Host host, String peerAddress) {
//        Multiaddr addr = Multiaddr.fromString(peerAddress);
//        host.getNetwork().connect(addr).thenAccept(conn -> {
//            System.out.println("Connected to " + peerAddress);
//            sendMessage(conn, "Hello from peer!");
//        }).exceptionally(e -> {
//            System.err.println("Connection failed: " + e.getMessage());
//            return null;
//        });
//    }
//
//    private static void sendMessage(Connection conn, String message) {
//        conn.newStream().thenAccept(stream -> {
//            try {
//                stream.writeAndFlush(message.getBytes());
//                System.out.println("Message sent: " + message);
//            } catch (Exception e) {
//                System.err.println("Error sending message: " + e.getMessage());
//            }
//        });
//    }
//}
