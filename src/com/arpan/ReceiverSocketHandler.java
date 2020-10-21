package com.arpan;

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

    public String receiveHandshake (String peerId) {
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        return peerConnection.waitForHandshake();
    }

    public void receiveHandshake (PeerConnection peerConnection) {
        String otherId = peerConnection.waitForHandshake();
        peerConnectionMap.put(otherId, peerConnection);
        peer.onNewHandshakeReceived(otherId, peerConnection);
    }

    private void startListeningServer() {
        new ListeningServer(peer.portNum, this).start();
    }

    public boolean isConnectedTo(String peerId) {
        return peerConnectionMap.containsKey(peerId);
    }
}
