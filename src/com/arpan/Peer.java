package com.arpan;

import com.arpan.log.LoggingThread;
import com.arpan.message.*;
import com.arpan.model.BitField;
import com.arpan.model.PeerInfo;
import com.arpan.model.State;
import com.arpan.socket.PeerConnection;
import com.arpan.socket.ReceiverSocketHandler;
import com.arpan.socket.SenderSocketHandler;
import com.arpan.timertask.OptimisticUnchokingTask;
import com.arpan.timertask.PreferredNeighborsTask;
import com.arpan.util.Config;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

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

    private List<String> preferredNeighbors = new ArrayList<>();

    private final Map<String, State> sendStateMap = new HashMap<>();
    private final Map<String, State> receiveStateMap = new HashMap<>();
    private final Map<String, BitField> peerBitfieldMap = new HashMap<>();
    private final Map<String, Boolean> iAmInterestedInMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> interestedInMeMap = new ConcurrentHashMap<>();
    public final Map<String, Float> peerDownloadRateMap = new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

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

            List<PeerInfo> peerInfoList = config.readPeerInfo(PEER_INFO_CONFIG);
            processPeerInfo(peerId, peerInfoList, num_pieces);
            bitField = new BitField(hasFile, num_pieces);

            String logFileName = "log_peer_" + peerId + ".log";
            LoggingThread loggingThread = new LoggingThread(logQueue, logFileName);
            executor.execute(loggingThread::startLogging);

            senderSocketHandler = new SenderSocketHandler(this);
            receiverSocketHandler = new ReceiverSocketHandler(this);
            Runnable receiver = () -> receiverSocketHandler.run();
            executor.execute(receiver);

            connectToPeers(peerInfoList);
            startTimerTasks(config);

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
                sendStateMap.put(peerInfo.peerId, State.NOT_CONNECTED);
                receiveStateMap.put(peerInfo.peerId, State.CHOKED);
                peerBitfieldMap.put(peerInfo.peerId, new BitField(false, num_pieces));
                iAmInterestedInMap.put(peerInfo.peerId, false);
                interestedInMeMap.put(peerInfo.peerId, false);
                peerDownloadRateMap.put(peerInfo.peerId, 0f);
            }
        }
    }

    private void startTimerTasks(Config config) {
        PreferredNeighborsTask preferredNeighborsTask = new PreferredNeighborsTask(this, config);
        new Timer().schedule(preferredNeighborsTask, 0, config.getUnchokingInterval() * 1000);

        OptimisticUnchokingTask optimisticUnchokingTask = new OptimisticUnchokingTask(this);
        new Timer().schedule(optimisticUnchokingTask, 0, config.getOptimisticUnchokingInterval() * 1000);
    }

    /**************  HANDSHAKE  ********************/

    public void onNewHandshakeReceived (String otherId, PeerConnection peerConnection) {
//        System.out.println("Received handshake from " + otherId);
        if (sendStateMap.get(otherId) == State.SENT_HANDSHAKE) {
            sendStateMap.put(otherId, State.CONNECTED);
        }
        else if (sendStateMap.get(otherId) == State.NOT_CONNECTED) {
            senderSocketHandler.setPeerConnection(otherId, peerConnection);
            log(String.format("Peer %s is connected from Peer %s.", this.peerId, otherId));
//            System.out.println("Sending handshake to " + otherId);
            senderSocketHandler.sendHandshake(otherId);
            sendStateMap.put(otherId, State.CONNECTED);
        }
        System.out.println(otherId + ": " + sendStateMap.get(otherId));
//        System.out.println("Has file? = " + hasFile);
        if (sendStateMap.get(otherId) == State.CONNECTED && hasFile) {
            sendBitfield(otherId);
        }
    }

    public void handshakeWithPeer(String otherId) {
//        System.out.println("Sending handshake to " + otherId);
        senderSocketHandler.sendHandshake(otherId);
        sendStateMap.put(otherId, State.SENT_HANDSHAKE);
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

    public void handleChokeMessage(String otherId) {
        log(String.format("Peer %s is choked by %s.", this.peerId, otherId));
        receiveStateMap.put(otherId, State.CHOKED);
    }

    public void handleUnchokeMessage(String otherId) {
        log(String.format("Peer %s is unchoked by %s.", this.peerId, otherId));
        receiveStateMap.put(otherId, State.UNCHOKED);
    }

    /**************  SEND MESSAGES  ********************/

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

    private void sendChoke(String otherId) {
        ChokeMessage chokeMessage = new ChokeMessage();
        senderSocketHandler.sendMessage(otherId, chokeMessage.getMessage());
        sendStateMap.put(otherId, State.CHOKED);
        System.out.println("Sent choke to " + otherId);
    }

    private void sendUnchoke(String otherId) {
        UnchokeMessage unchokeMessage = new UnchokeMessage();
        senderSocketHandler.sendMessage(otherId, unchokeMessage.getMessage());
        sendStateMap.put(otherId, State.UNCHOKED);
        System.out.println("Sent unchoke to " + otherId);
    }

    public void setPreferredNeighbors(List<String> preferredNeighbors) {
        List<String> previouslyPreferredNeighbors = this.preferredNeighbors;
        this.preferredNeighbors = preferredNeighbors;

        if (!preferredNeighbors.isEmpty()) {
            String preferredNeighborsString = String.join(", ", preferredNeighbors);
            log(String.format("Peer %s has the preferred neighbors %s.", this.peerId, preferredNeighborsString));
        }

        previouslyPreferredNeighbors.stream()
                .filter(neighbor -> !preferredNeighbors.contains(neighbor))
                .forEach(this::sendChoke);

        preferredNeighbors.stream()
                .filter(neighbor -> !previouslyPreferredNeighbors.contains(neighbor))
                .forEach(this::sendUnchoke);
    }

    public void setOptimisticallyUnchokedNeighbor(String otherId) {
        log(String.format("Peer %s has the optimistically unchoked neighbor %s.", this.peerId, otherId));
        sendUnchoke(otherId);
    }

    public List<String> getPeersInterestedInMe() {
        return interestedInMeMap.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> getSendChokedPeers() {
        return sendStateMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(State.CHOKED))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void log(String message) {
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean hasCompleteFile() {
        return bitField.hasCompleteFile();
    }

    public String getPeerId() {
        return peerId;
    }
}
