package com.arpan;

import com.arpan.message.BitfieldMessage;

import java.io.IOException;
import java.util.Arrays;
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

    private ReceiverSocketHandler receiverSocketHandler;
    private SenderSocketHandler senderSocketHandler;

    private Map<String, PeerInfo> peerInfoMap;
    private Map<String, State> peerStateMap;
    private Map<String, BitField> peerBitfieldMap;

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

            List<PeerInfo> peerInfoList = config.readPeerInfo(peerId, PEER_INFO_CONFIG);
            processPeerInfo(peerId, peerInfoList, num_pieces);
            bitField = new BitField(hasFile, num_pieces);

            senderSocketHandler = new SenderSocketHandler(this);
            receiverSocketHandler = new ReceiverSocketHandler(this, peerId);

            connectToPeers(peerInfoMap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onNewHandshakeReceived (String otherId) {
        System.out.println("Received handshake from " + otherId);
        if (peerStateMap.get(otherId) == State.SENT_HANDSHAKE) {
            peerStateMap.put(otherId, State.CONNECTED);
            receiverSocketHandler.startReceivingLoop(otherId);
        }
        else if (peerStateMap.get(otherId) == State.NOT_CONNECTED) {
            senderSocketHandler.connectToPeerBlocking(peerInfoMap.get(otherId));
            System.out.println("Sending handshake to " + otherId);
            senderSocketHandler.sendHandshake(otherId);
            peerStateMap.put(otherId, State.CONNECTED);
        }
        if (peerStateMap.get(otherId) == State.CONNECTED && hasFile) {
            sendBitfield(otherId);
        }
    }

    public void handshakeWithPeer(String otherId) {
        System.out.println("Sending handshake to " + otherId);
        senderSocketHandler.sendHandshake(otherId);
        peerStateMap.put(otherId, State.SENT_HANDSHAKE);
    }

    public void onReceivedBitfieldMessage(String otherId, BitfieldMessage message) {
        System.out.println("Received Bitfield message from " + otherId);
        BitField peerBitfield = peerBitfieldMap.get(otherId);
        peerBitfield.setBits(message.getBitfield());
    }

    private void sendBitfield(String peerId) {
        System.out.println(Arrays.toString(bitField.toByteArray()));
        BitfieldMessage bitfieldMessage = new BitfieldMessage(bitField.toByteArray());
        senderSocketHandler.sendMessage(peerId, bitfieldMessage.getMessage());
        System.out.println("Sent bitfield to " + peerId);
    }

    private void connectToPeers(Map<String, PeerInfo> peerInfoMap) {
        for (String otherId : peerInfoMap.keySet()) {
            if (senderSocketHandler.connectToPeerBlocking(peerInfoMap.get(otherId))) {
                System.out.println(peerId + " connected to " + otherId);
                handshakeWithPeer(otherId);
            }
        }
    }

    private void processPeerInfo(String peerId, List<PeerInfo> peerInfoList, int num_pieces) {
        peerInfoMap = new HashMap<>();
        peerStateMap = new HashMap<>();
        peerBitfieldMap = new HashMap<>();
        for (PeerInfo peerInfo : peerInfoList) {
            if (peerId.equals(peerInfo.peerId)) {
                this.portNum = peerInfo.portNum;
                this.hasFile |= peerInfo.hasFile;
            } else {
                peerInfoMap.put(peerInfo.peerId, peerInfo);
                peerStateMap.put(peerInfo.peerId, State.NOT_CONNECTED);
                peerBitfieldMap.put(peerInfo.peerId, new BitField(false, num_pieces));
            }
        }
    }

    public String getPeerId() {
        return peerId;
    }
}
