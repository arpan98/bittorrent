package com.arpan;

import com.arpan.log.LoggingThread;
import com.arpan.model.BitField;
import com.arpan.model.FilePiece;
import com.arpan.model.PeerInfo;
import com.arpan.model.State;
import com.arpan.socket.PeerClient;
import com.arpan.socket.PeerServer;
import com.arpan.util.Config;
import com.arpan.util.FileUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Peer {

    private static final String COMMON_CONFIG = "cfg/Common.cfg";
    private static final String PEER_INFO_CONFIG = "cfg/PeerInfo.cfg";
    private static final String FOLDER_PATH = "peer_";
    private final String peerId;
    public int portNum;
    private boolean hasFile;
    private BitField bitField;
    private FilePiece[] filePieces;
    private String folderPath;
    private FileUtil fileUtil;
    public Config config;
    private LoggingThread loggingThread;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
/*
    private ReceiverSocketHandler receiverSocketHandler;
    private SenderSocketHandler senderSocketHandler;*/

    private List<String> preferredNeighbors = new ArrayList<>();

    private final Map<String, State> sendStateMap = new HashMap<>();
    private final Map<String, State> receiveStateMap = new HashMap<>();
    private final Map<String, BitField> peerBitfieldMap = new HashMap<>();
    private final Map<String, Boolean> iAmInterestedInMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> interestedInMeMap = new ConcurrentHashMap<>();
    private final Map<String, Float> peerDownloadRateMap = new ConcurrentHashMap<>();
    private Map<String, PeerInfo> peerInfoMap = new ConcurrentHashMap<>();
    private PeerClient peerClient;
    private PeerServer peerServer;
    private Set<Integer> requestedPieces = ConcurrentHashMap.newKeySet();

    private final LinkedBlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    public Peer(String peerId) {
        this.peerId = peerId;
    }

    public void run() {
        initializePeer();
    }

    /**************  INITIALIZATION  ********************/

    private void initializePeer() {

        config = new Config();
        try {
            config.readCommon(COMMON_CONFIG);
            int num_pieces = (int) Math.ceil((double) config.getFileSize() / config.getPieceSize());

            this.peerInfoMap = config.readPeerInfo(PEER_INFO_CONFIG);
            processPeerInfo(peerId, peerInfoMap, num_pieces);
            bitField = new BitField(hasFile, num_pieces);
            filePieces = new FilePiece[num_pieces]; //array storing the bytes for each piece.
            // create folder for peer
            folderPath = FOLDER_PATH + peerId;
            String logFileName = "log_peer_" + peerId + ".log";
            loggingThread = new LoggingThread(logQueue, logFileName);
            executor.execute(loggingThread::startLogging);

            File file = new File(folderPath);
            if (file.mkdir()) {
//                log(String.format("Directory created successfully for  %s ", this.getPeerId()));

            } else {
//                log(String.format("Could not creat directory for %s ", this.getPeerId()));

            }

            //if peer has file, add data file to its folder and copy bytes to filePieces array
            fileUtil = new FileUtil(peerId, config.getFileName());
            if (hasFile) {
//                fileUtil.copyContent(peerId);
                filePieces = fileUtil.getPieces(peerId, num_pieces, config.getPieceSize());
            }




            Map<String, PeerInfo> connectedPeerMap = new ConcurrentHashMap<>();
            peerClient = new PeerClient(this, peerInfoMap, connectedPeerMap);
            peerClient.start();


            peerServer = new PeerServer(this, peerInfoMap, connectedPeerMap);
            peerServer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void exit(){
//        log("Peer is exiting.");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        System.out.println("All peers have files");
//        System.out.println("EXIT");
//        peerServer.kill();
//        peerServer.stop();
//        peerClient.stop();
        System.exit(0);
        //write exit code here ///

    }
    private void processPeerInfo(String peerId, Map<String, PeerInfo> peerInfoMap, int num_pieces) {
        for (String key : peerInfoMap.keySet()) {
            if (peerId.equals(key)) {
                this.portNum = peerInfoMap.get(key).portNum;
                this.hasFile |= peerInfoMap.get(key).hasFile;
            } else {
                sendStateMap.put(key, State.NOT_CONNECTED);
                receiveStateMap.put(key, State.CHOKED);
                peerBitfieldMap.put(key, new BitField(false, num_pieces));
                iAmInterestedInMap.put(key, false);
                interestedInMeMap.put(key, false);
                peerDownloadRateMap.put(key, 0f);
            }
        }
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

    public Integer findNextPieceNeeded(String otherId) {
        for (int i = 0; i < this.filePieces.length; i++) {
            if (!this.bitField.getBitFieldBit(i) && peerBitfieldMap.get(otherId).getBitFieldBit(i)
                    && !requestedPieces.contains(i)) {
                return i;
            }
        }
        return null;
    }

    public FilePiece[] getFilePieces() {
        return filePieces;
    }

    public String getPeerId() {
        return peerId;
    }

    public boolean getHasFile() {
        return hasFile;
    }

    public BitField getBitField() {
        return bitField;
    }

    public Map<String, BitField> getPeerBitfieldMap() {
        return peerBitfieldMap;
    }

    public Map<String, Boolean> getInterestedInMeMap() {
        return interestedInMeMap;
    }

    public Map<String, Float> getPeerDownloadRateMap() {
        return peerDownloadRateMap;
    }

    public Map<String, State> getSendStateMap() {
        return sendStateMap;
    }

    public List<String> getPreferredNeighbors() {
        return preferredNeighbors;
    }

    public void setPreferredNeighbors(List<String> neighbors) {
        preferredNeighbors = neighbors;
    }

    public Map<String, State> getReceiveStateMap() {
        return receiveStateMap;
    }

    public Set<Integer> getRequestedPieces() {
        return requestedPieces;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    public FileUtil getFileUtil() {
        return fileUtil;
    }

    public void setSendState(String otherId, State state) {
        sendStateMap.put(otherId, state);
    }

    public Map<String, PeerInfo> getPeerInfoMap() {
        return peerInfoMap;
    }
}
