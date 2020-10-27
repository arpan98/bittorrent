package com.arpan;

import com.arpan.message.HandshakeMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SenderSocketHandler {
    private final Peer peer;
    private final String selfId;

    private final Map<String, PeerConnection> peerConnectionMap;

    public SenderSocketHandler(Peer peer) {
        this.selfId = peer.getPeerId();
        this.peer = peer;
        this.peerConnectionMap = new ConcurrentHashMap<>();
    }

    public PeerConnection connectToPeerBlocking(PeerInfo peerInfo) {
        if (selfId.equals(peerInfo.peerId))
            return null;
        else if (peerConnectionMap.containsKey(peerInfo.peerId))
            return null;
        else {
            PeerConnection peerConnection = new PeerConnection();
            if (peerConnection.connect(peerInfo.hostName, peerInfo.portNum)) {
                peerConnectionMap.put(peerInfo.peerId, peerConnection);
                return peerConnection;
            }
        }
        return null;
    }

    public void sendHandshake(String peerId) {
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        if (peerConnection != null) {
            HandshakeMessage handshakeMessage = new HandshakeMessage(selfId);
            peerConnection.sendHandshake(handshakeMessage);
        } else {
            System.out.println("Not connected to " + peerId);
        }
    }

    public void sendMessage(String peerId, byte[] message) {
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        if (peerConnection != null) {
            peerConnection.sendMessage(message);
        } else {
            System.out.println("Not connected to " + peerId);
        }
    }

    public void setPeerConnection(String peerId, PeerConnection peerConnection) {
        peerConnectionMap.put(peerId, peerConnection);
    }
}
