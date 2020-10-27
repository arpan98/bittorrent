package com.arpan;

import com.arpan.message.BitfieldMessage;
import com.arpan.message.MessageType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReceiverSocketHandler {
    private final Peer peer;
    private final String selfId;

    private final Map<String, PeerConnection> peerConnectionMap;

    public ReceiverSocketHandler(Peer peer, String selfId) {
        this.peer = peer;
        this.selfId = selfId;
        this.peerConnectionMap = new ConcurrentHashMap<>();
    }

    public void run() {
        startListeningServer();
        startReceivingLoop();
    }

    public void onReceivedConnection(PeerConnection peerConnection) {
        receiveHandshake(peerConnection);
    }

    public void receiveHandshake (String peerId) {
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        receiveHandshake(peerConnection);
    }

    public void receiveHandshake (PeerConnection peerConnection) {
        String otherId = peerConnection.waitForHandshake();
        peerConnectionMap.put(otherId, peerConnection);
        peer.onNewHandshakeReceived(otherId, peerConnection);
    }

    public void onReceivedMessage(String peerId, byte typeByte, byte[] messagePayload) {
        if (typeByte == MessageType.CHOKE.getValue()) {

        }
        else if (typeByte == MessageType.UNCHOKE.getValue()) {

        }
        else if (typeByte == MessageType.INTERESTED.getValue()) {

        }
        else if (typeByte == MessageType.NOT_INTERESTED.getValue()) {

        }
        else if (typeByte == MessageType.HAVE.getValue()) {

        }
        else if (typeByte == MessageType.BITFIELD.getValue()) {
            BitfieldMessage bitfieldMessage = new BitfieldMessage(messagePayload);
            peer.handleBitfieldMessage(peerId, bitfieldMessage);
        }
        else if (typeByte == MessageType.REQUEST.getValue()) {

        }
        else if (typeByte == MessageType.PIECE.getValue()) {

        }
    }

    public void setPeerConnection(String peerId, PeerConnection peerConnection) {
        peerConnectionMap.put(peerId, peerConnection);
    }

    private void startListeningServer() {
        new ListeningServer(peer.portNum, this).start();
    }

    private void receiveMessage(String peerId) {
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        peerConnection.tryReceive();
    }

    public void startReceivingLoop() {
        while (true) {
            for (PeerConnection peerConnection : peerConnectionMap.values()) {
                peerConnection.tryReceive();
            }
        }
    }
}
