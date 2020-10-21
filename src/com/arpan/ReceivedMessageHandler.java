package com.arpan;

import com.arpan.message.HandshakeMessage;

public class ReceivedMessageHandler extends Thread {

    private final PeerConnection connection;

    private String selfId, peerId;

    public ReceivedMessageHandler(PeerConnection connection, String selfId) {
        this.connection = connection;
        this.selfId = selfId;
    }

    @Override
    public void run() {
        System.out.println("Receive handler started for " + this.selfId);
        try{
            //initialize Input and Output streams
            while(true)
            {
                //receive the message sent from the client
                if (!connection.isHandshakeDone()) {
                    this.peerId = connection.waitForHandshake();
                    System.out.println(selfId + ": Received handshake from " + this.peerId);
                    HandshakeMessage sendHandshakeMessage = new HandshakeMessage(selfId);
                    connection.sendHandshake(sendHandshakeMessage);
                    System.out.println(selfId + ": Sent handshake to " + peerId);
                } else {

                }

                //show the message to the user
//                System.out.println("Receive message: " + message + " from client " + clientNum);
//                //Capitalize all letters in the message
//                MESSAGE = message.toUpperCase();
//                //send MESSAGE back to the client
//                connection.sendMessage(MESSAGE);
            }
        } finally{
            //Close connections
            connection.close();
        }
    }
}
