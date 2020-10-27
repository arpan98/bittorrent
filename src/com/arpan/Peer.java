package com.arpan;

import com.arpan.log.LoggingThread;
import com.arpan.message.BitfieldMessage;
import com.arpan.message.InterestedMessage;
import com.arpan.message.NotInterestedMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer {

    private static final String COMMON_CONFIG = "cfg/Common.cfg";
    private static final String PEER_INFO_CONFIG = "cfg/PeerInfo.cfg";

    private final String peerId;
    public int portNum;
    private boolean hasFile;
    private BitField bitField;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private ReceiverSocketHandler receiverSocketHandler;
    private SenderSocketHandler senderSocketHandler;

    private final Map<String, State> peerStateMap = new HashMap<>();;
    private final Map<String, BitField> peerBitfieldMap = new HashMap<>();
    private final Map<String, Boolean> iAmInterestedInMap = new HashMap<>();
    private final Map<String, Boolean> interestedInMeMap = new HashMap<>();

    LinkedBlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    public Peer(String peerId) {
        this.peerId = peerId;
    }

    public void run() {
        initializePeer();
    }

    /**************  INITIALIZATION  ********************/

    private void initializePeer() {

        Config config = new Config();
        try {
            config.readCommon(COMMON_CONFIG);
            int num_pieces = (int) Math.ceil((double)config.getFileSize() / config.getPieceSize());

            List<PeerInfo> peerInfoList = config.readPeerInfo(peerId, PEER_INFO_CONFIG);
            processPeerInfo(peerId, peerInfoList, num_pieces);
            bitField = new BitField(hasFile, num_pieces);

            String logFileName = "log_peer_" + peerId + ".log";
            LoggingThread loggingThread = new LoggingThread(logQueue, logFileName);
            executor.execute(loggingThread::startLogging);

            senderSocketHandler = new SenderSocketHandler(this);
            receiverSocketHandler = new ReceiverSocketHandler(this, peerId);
            Runnable receiver = () -> receiverSocketHandler.run();
            executor.execute(receiver);

            connectToPeers(peerInfoList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToPeers(List<PeerInfo> peerInfoList) {
        for (PeerInfo peerInfo : peerInfoList) {
            String otherId = peerInfo.peerId;
            if (otherId.equals(this.peerId))
                break;
            PeerConnection peerConnection = senderSocketHandler.connectToPeerBlocking(peerInfo);
            if (peerConnection != null) {
                peerConnection.setReceiverSocketHandler(receiverSocketHandler);
                log(String.format("Peer %s makes a connection to Peer %s.", this.peerId, otherId));
                receiverSocketHandler.setPeerConnection(otherId, peerConnection);
                handshakeWithPeer(otherId);
            }
        }
    }

    private void processPeerInfo(String peerId, List<PeerInfo> peerInfoList, int num_pieces) {
        for (PeerInfo peerInfo : peerInfoList) {
            if (peerId.equals(peerInfo.peerId)) {
                this.portNum = peerInfo.portNum;
                this.hasFile |= peerInfo.hasFile;
            } else {
                peerStateMap.put(peerInfo.peerId, State.NOT_CONNECTED);
                peerBitfieldMap.put(peerInfo.peerId, new BitField(false, num_pieces));
                iAmInterestedInMap.put(peerInfo.peerId, false);
                interestedInMeMap.put(peerInfo.peerId, false);
            }
        }
    }

    /**************  HANDSHAKE  ********************/

    public void onNewHandshakeReceived (String otherId, PeerConnection peerConnection) {
//        System.out.println("Received handshake from " + otherId);
        if (peerStateMap.get(otherId) == State.SENT_HANDSHAKE) {
            peerStateMap.put(otherId, State.CONNECTED);
        }
        else if (peerStateMap.get(otherId) == State.NOT_CONNECTED) {
            senderSocketHandler.setPeerConnection(otherId, peerConnection);
            log(String.format("Peer %s is connected from Peer %s.", this.peerId, otherId));
//            System.out.println("Sending handshake to " + otherId);
            senderSocketHandler.sendHandshake(otherId);
            peerStateMap.put(otherId, State.CONNECTED);
        }
        System.out.println(otherId + ": " + peerStateMap.get(otherId));
//        System.out.println("Has file? = " + hasFile);
        if (peerStateMap.get(otherId) == State.CONNECTED && hasFile) {
            sendBitfield(otherId);
        }
    }

    public void handshakeWithPeer(String otherId) {
//        System.out.println("Sending handshake to " + otherId);
        senderSocketHandler.sendHandshake(otherId);
        peerStateMap.put(otherId, State.SENT_HANDSHAKE);
        receiverSocketHandler.receiveHandshake(otherId);
    }

    /**************  HANDLE MESSAGES  ********************/

    public void handleBitfieldMessage(String otherId, BitfieldMessage message) {
        System.out.println("Received Bitfield message from " + otherId);
        BitField peerBitfield = peerBitfieldMap.get(otherId);
        peerBitfield.setBits(message.getBitfield());

        if (peerBitfield.hasExtraBits(this.bitField))
            sendInterested(otherId);
        else
            sendNotInterested(otherId);
    }

    public void handleInterestedMessage(String otherId) {
//        System.out.println("Received Interested message from " + otherId);
        log(String.format("Peer %s received the 'interested' message from %s.", this.peerId, otherId));
        interestedInMeMap.put(otherId, true);
    }

    public void handleNotInterestedMessage(String otherId) {
//        System.out.println("Received Not Interested message from " + otherId);
        log(String.format("Peer %s received the 'not interested' message from %s.", this.peerId, otherId));
        interestedInMeMap.put(otherId, false);
    }

    private void sendInterested(String otherId) {
        InterestedMessage interestedMessage = new InterestedMessage();
        senderSocketHandler.sendMessage(otherId, interestedMessage.getMessage());
        iAmInterestedInMap.put(otherId, true);
        System.out.println("Sent Interested to " + otherId);
    }

    private void sendNotInterested(String otherId) {
        NotInterestedMessage notInterestedMessage = new NotInterestedMessage();
        senderSocketHandler.sendMessage(otherId, notInterestedMessage.getMessage());
        iAmInterestedInMap.put(otherId, false);
        System.out.println("Sent Not Interested to " + otherId);
    }

    private void sendBitfield(String otherId) {
        BitfieldMessage bitfieldMessage = new BitfieldMessage(bitField.toByteArray());
        senderSocketHandler.sendMessage(otherId, bitfieldMessage.getMessage());
        System.out.println("Sent bitfield to " + otherId);
    }

    public void log(String message) {
        System.out.println(logQueue.toString());
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getPeerId() {
        return peerId;
    }
}
