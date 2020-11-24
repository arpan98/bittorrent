package com.arpan;

import com.arpan.log.LoggingThread;
import com.arpan.message.*;


import java.io.IOException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer {

    private static final String COMMON_CONFIG = "cfg/Common.cfg";
    private static final String PEER_INFO_CONFIG = "cfg/PeerInfo.cfg";
    private static final String FOLDER_PATH = "peers/";
    private final String peerId;
    public int portNum;
    private boolean hasFile;
    private BitField bitField;
    private FilePiece[] filePieces;
    private String folderPath;
    private FileUtil fileUtil;

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
            filePieces = new FilePiece[num_pieces]; //array storing the bytes for each piece.
            // create folder for peer
            folderPath = FOLDER_PATH + peerId;
            File file = new File(folderPath);
            if(file.mkdir()){
                System.out.println("Directory created successfully for " + peerId);
            }else{
                System.out.println("Sorry couldn’t create specified directory for "+ peerId);
            }

            //if peer has file, add data file to its folder and copy bytes to filePieces array
            System.out.println(hasFile);
            fileUtil = new FileUtil(peerId);
            if(hasFile){
                fileUtil.copyContent(peerId);
                filePieces = fileUtil.getPieces(peerId, num_pieces, config.getPieceSize());
            }


            String logFileName = folderPath+ "/log_peer_" + peerId + ".log";
            LoggingThread loggingThread = new LoggingThread(logQueue, logFileName);
            executor.execute(loggingThread::startLogging);

            senderSocketHandler = new SenderSocketHandler(this);
            receiverSocketHandler = new ReceiverSocketHandler(this, peerId);
            Runnable receiver = () -> receiverSocketHandler.run();
            executor.execute(receiver);

            connectToPeers(peerInfoList);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
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

    public Integer handlePieceMessage(String otherId, PieceMessage message) {
        byte[] messagePayload = message.getPieceMessage();

        Integer pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(messagePayload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
        BitField peerBitfield = peerBitfieldMap.get(otherId);
        if (peerBitfield.getBitFieldBit(pieceIndex) && !this.bitField.getBitFieldBit(pieceIndex)) {
            byte[] piece = Arrays.copyOfRange(messagePayload, 4, messagePayload.length);
            filePieces[pieceIndex] = new FilePiece(piece); // added piece to filePieces

            //Set bit to 1
            this.bitField.setBitFieldBit(pieceIndex);

            // If all are received, merge the file.
            if(this.bitField.getCardinality() == this.filePieces.length) // checks if all bits are set
                try{
                    fileUtil.constructFile(this);
                    hasFile = true;
                }
                catch(IOException e){
                    System.out.println("Error in creating file");
                    e.printStackTrace();
                }
            return pieceIndex;
        }
        return null;
    }

    public void broadcastHaveRequest(){
        BitfieldMessage bitfieldMessage = new BitfieldMessage(bitField.toByteArray());
        for(String connectedPeer : peerStateMap.keySet()){
            senderSocketHandler.sendMessage(connectedPeer, bitfieldMessage.getMessage());
            System.out.println("Sent bitfield to " + connectedPeer);
        }
    }
    public void sendRequest(String otherId){
        Integer nextPieceNeeded = findNextPieceNeeded(otherId);
        System.out.println(this.peerId + " requesting "+nextPieceNeeded+"th bit from "+otherId);

        if (nextPieceNeeded != null) {
            // Send the request message for the next piece needed
            RequestMessage requestMessage = new RequestMessage(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(nextPieceNeeded).array());
            senderSocketHandler.sendMessage(otherId, requestMessage.getRequestMessage());
        }
    }

    public void handleRequestMessage(String otherId, RequestMessage requestMessage){
        if (true) { //check for choke or optimistic
            int pieceRequired = ByteBuffer.wrap(requestMessage.getRequestMessage()).order(ByteOrder.BIG_ENDIAN).getInt();
            byte[] pieceRequestedInBytes = requestMessage.getRequestMessage();
            byte[] payLoad = filePieces[pieceRequired].getData();


            ByteBuffer messagePayload = ByteBuffer.allocate(pieceRequestedInBytes.length + payLoad.length);
            messagePayload.put(pieceRequestedInBytes, 0, pieceRequestedInBytes.length);
            messagePayload.put(payLoad, 0, payLoad.length);
            PieceMessage pieceMessage = new PieceMessage(messagePayload.array());
            senderSocketHandler.sendMessage(otherId, pieceMessage.getPieceMessage());
        }
    }

    private  Integer findNextPieceNeeded(String otherId) {
        for (int i = 0; i < this.filePieces.length ; i++) {
            if (!this.bitField.getBitFieldBit(i) && peerBitfieldMap.get(otherId).getBitFieldBit(i)) {
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

    public boolean getHashFile(){
        return hasFile;
    }
}
