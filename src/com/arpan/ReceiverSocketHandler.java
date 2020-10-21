package com.arpan;

import com.arpan.message.BitfieldMessage;
import com.arpan.message.MessageType;

import java.util.HashMap;
import java.util.Map;

public class ReceiverSocketHandler {
    private final Peer peer;
    private final String selfId;

    private final Map<String, PeerConnection> peerConnectionMap;

    public ReceiverSocketHandler(Peer peer, String selfId) {
        this.peer = peer;
        this.selfId = selfId;
        this.peerConnectionMap = new HashMap<>();

        startListeningServer();
    }

    public void onReceivedConnection(PeerConnection peerConnection) {
        receiveHandshake(peerConnection);
    }

    public void receiveHandshake (PeerConnection peerConnection) {
        String otherId = peerConnection.waitForHandshake();
        peerConnectionMap.put(otherId, peerConnection);
        peer.onNewHandshakeReceived(otherId);
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
            peer.onReceivedBitfieldMessage(peerId, bitfieldMessage);
        }
        else if (typeByte == MessageType.REQUEST.getValue()) {

        }
        else if (typeByte == MessageType.PIECE.getValue()) {

        }
    }

    private void startListeningServer() {
        new ListeningServer(peer.portNum, this).start();
    }

    public void startReceivingLoop(String peerId) {
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        peerConnection.startReceiveLoop();
    }

    public boolean isConnectedTo(String peerId) {
        return peerConnectionMap.containsKey(peerId);
    }
}
