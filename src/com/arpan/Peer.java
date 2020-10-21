package com.arpan;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Peer {

    private static final String COMMON_CONFIG = "cfg/Common.cfg";
    private static final String PEER_INFO_CONFIG = "cfg/PeerInfo.cfg";

    private final String peerId;
    public int portNum;
    private boolean hasFile;
    private BitField bitField;

    private Map<String, PeerInfo> peerInfoMap;
    private ReceiverSocketHandler receiverSocketHandler;
    private SenderSocketHandler senderSocketHandler;

    public Peer(String peerId) {
        this.peerId = peerId;
    }

    public void run() {
        initializePeer();
    }

    private void initializePeer() {

        Config config = new Config();
        try {
            config.readCommon(COMMON_CONFIG);
            int num_pieces = (int) Math.ceil((double)config.getFileSize() / config.getPieceSize());
            bitField = new BitField(hasFile, num_pieces);

            List<PeerInfo> peerInfoList = config.readPeerInfo(peerId, PEER_INFO_CONFIG);
            this.peerInfoMap = processPeerInfo(peerId, peerInfoList);

            senderSocketHandler = new SenderSocketHandler(this);
            receiverSocketHandler = new ReceiverSocketHandler(this, peerId);

            connectToPeers(peerInfoList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToPeers(List<PeerInfo> peerInfoList) {
        for (PeerInfo peerInfo : peerInfoList) {
            if (senderSocketHandler.connectToPeerBlocking(peerInfo) && receiverSocketHandler.isConnectedTo(peerId)) {
                System.out.println(peerId + " connected to " + peerInfo.peerId);
            }
        }
    }

    private Map<String, PeerInfo> processPeerInfo(String peerId, List<PeerInfo> peerInfoList) {
        Map<String, PeerInfo> peerMap = new HashMap<>();
        for (PeerInfo peerInfo : peerInfoList) {
            if (peerId.equals(peerInfo.peerId)) {
                this.portNum = peerInfo.portNum;
                this.hasFile |= peerInfo.hasFile;
            } else {
                peerMap.put(peerInfo.peerId, peerInfo);
            }
        }
        return peerMap;
    }

    public void onNewHandshakeReceived (String otherId, PeerConnection peerConnection) {
        senderSocketHandler.setPeerConnection(otherId, peerConnection);
        senderSocketHandler.handshakeWithPeer(peerId);
    }

    public void connectToPeer(String otherId) {
        senderSocketHandler.handshakeWithPeer(otherId);
        String otherIdRecv = receiverSocketHandler.receiveHandshake(otherId);
        System.out.println(peerId + ": Received handshake from " + otherIdRecv);
        if (!otherIdRecv.equals(otherId)) {
            System.out.println("Sent handshake to " + otherId + " but received handshake from " + otherId);
        }
    }

    public String getPeerId() {
        return peerId;
    }

    public BitField getBitField() {
        return bitField;
    }
}
