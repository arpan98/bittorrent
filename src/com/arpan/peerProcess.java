package com.arpan;

public class peerProcess {

    public static void main(String[] args) {
        System.out.println("Starting Client with Peer ID: " + args[0]);
        Peer peer = new Peer(args[0]);
        peer.run();
    }
}
