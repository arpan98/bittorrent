package com.arpan;

import com.arpan.message.MessageType;

public class Main {

    public static void main(String[] args) {
        MessageType messageType = MessageType.HAVE;
	    System.out.println(messageType.getValue());

        Peer peer = new Peer(args[0]);
        peer.run();
        System.out.println("Started Client with Peer ID: " + args[0]);
    }
}
