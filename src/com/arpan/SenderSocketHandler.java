package com.arpan;

import com.arpan.message.BitfieldMessage;
import com.arpan.message.HandshakeMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SenderSocketHandler {
    private final Peer peer;
    private final String selfId;

    private final Map<String, PeerConnection> peerConnectionMap;

    public SenderSocketHandler(Peer peer) {
        this.selfId = peer.getPeerId();
        this.peer = peer;
        this.peerConnectionMap = new HashMap<>();
    }

    public boolean connectToPeerBlocking(PeerInfo peerInfo) {
        if (selfId.equals(peerInfo.peerId))
            return false;
        else if (peerConnectionMap.containsKey(peerInfo.peerId))
            return true;
        else {
            PeerConnection peerConnection = new PeerConnection();
            if (peerConnection.connect(peerInfo.hostName, peerInfo.portNum)) {
                peerConnectionMap.put(peerInfo.peerId, peerConnection);
                return true;
            }
        }
        return false;
    }

    public void handshakeWithPeer(String peerId) {
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        if (peerConnection != null) {
            HandshakeMessage handshakeMessage = new HandshakeMessage(peerId);
            peerConnection.sendHandshake(handshakeMessage);
            System.out.println(selfId + ": Sent handshake to " + selfId);
        } else {
            System.out.println("Not connected to " + peerId);
        }
    }

    private void sendBitfield(PeerConnection peerConnection, String peerId) {
        byte[] bitfield = peer.getBitField().toByteArray();
        BitfieldMessage bitfieldMessage = new BitfieldMessage(bitfield);
        peerConnection.sendMessage(bitfieldMessage.getMessage());
        System.out.println("Sent bitfield to " + peerId);
    }

    public void setPeerConnection(String peerId, PeerConnection peerConnection) {
        peerConnectionMap.put(peerId, peerConnection);
    }
}
