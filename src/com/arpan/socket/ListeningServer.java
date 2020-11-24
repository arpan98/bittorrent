package com.arpan.socket;

import com.arpan.socket.PeerConnection;
import com.arpan.socket.ReceiverSocketHandler;

import java.io.IOException;
import java.net.ServerSocket;

public class ListeningServer extends Thread {
    private final int portNum;

    private final ReceiverSocketHandler receiverSocketHandler;

    public ListeningServer(int portNum, ReceiverSocketHandler receiverSocketHandler) {
        this.portNum = portNum;
        this.receiverSocketHandler = receiverSocketHandler;
    }

    public void run() {
        try (ServerSocket listener = new ServerSocket(this.portNum)) {
            while (true) {
                PeerConnection peerConnection = new PeerConnection(listener.accept(), receiverSocketHandler);
                receiverSocketHandler.onReceivedConnection(peerConnection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
