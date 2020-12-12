package com.arpan.util;

import com.arpan.Peer;
import com.arpan.message.*;
import com.arpan.model.BitField;
import com.arpan.model.FilePiece;
import com.arpan.model.PeerInfo;
import com.arpan.model.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

public class MessageUtil {
    public static String HandShakeAndResponse(DataInputStream in, DataOutputStream out, String peerId)
            throws IOException, InterruptedException {
        HandshakeMessage handshakeMessage = new HandshakeMessage();
        while (true) {
            if (handshakeMessage.recvHandshake(in, null))
                break;
        }
        HandshakeMessage outHandshakeMessage = new HandshakeMessage(peerId);
        outHandshakeMessage.sendHandshake(out);
        return handshakeMessage.getPeerId();
    }

    public static void sendBitfield(DataOutputStream out, BitSet bitField)
            throws IOException, InterruptedException{
        BitfieldMessage bitfieldMessage = new BitfieldMessage(bitField.toByteArray());
        BitSet receivedBitSet = BitSet.valueOf(bitfieldMessage.getBitfield());
        //System.out.println("rec in send:::::" + receivedBitSet.toString());
        bitfieldMessage.sendBitfield(out);
       // System.out.println("Bitfield sent!!");
    }

    public static void handleBitfieldMessage(Peer host, PeerInfo peer, BitfieldMessage message, DataOutputStream outputStream)
        throws IOException {
        BitField peerBitfield = host.getPeerBitfieldMap().get(peer.peerId);
        //System.out.println(message.getBitfield());
        BitSet receivedBitSet = BitSet.valueOf(message.getBitfield());
        peerBitfield.setBitField(receivedBitSet);
        host.getPeerBitfieldMap().put(peer.peerId, peerBitfield);
        //System.out.println("recived bitset from " +peer.peerId +" "+BitSet.valueOf(host.getPeerBitfieldMap().get(peer.peerId).getBitField().toByteArray()).toString()) ;
//        host.log(String.format("Peer %s received bitfield message from %s - %s.", host.getPeerId(), peer.peerId, receivedBitSet.toString()));

        if (peerBitfield.hasExtraBits(host.getBitField())) {
            InterestedMessage interestedMessage = new InterestedMessage();
            interestedMessage.sendInterestedMessage(outputStream);
//            host.log(String.format("Peer %s sent interested message to %s ", host.getPeerId(), peer.peerId));
        } else {
            NotInterestedMessage notInterestedMessage = new NotInterestedMessage();
            notInterestedMessage.sendNotInterestedMessage(outputStream);
//            host.log(String.format("Peer %s sent not interested message to %s ", host.getPeerId(), peer.peerId));
        }
    }

    public static void handleInterestedMessage(Peer host, PeerInfo peer, DataOutputStream outputStream){
        peer.isInterested = true;
        host.getInterestedInMeMap().put(peer.peerId,true);
        host.log(String.format("Peer %s received the 'interested' message from %s ", host.getPeerId(), peer.peerId));

    }
    public static void handleNotInterestedMessage(Peer host, PeerInfo peer, DataOutputStream outputStream){
        peer.isInterested = false;
        host.getInterestedInMeMap().put(peer.peerId,false);
        host.log(String.format("Peer %s received the 'not interested' message from %s ", host.getPeerId(), peer.peerId));
    }
    public static void handleChokeMessage(Peer host, PeerInfo peer, DataOutputStream outputStream) {
        host.log(String.format("Peer %s is choked by %s.", host.getPeerId(), peer.peerId));
        System.out.println(host.getPeerId() +" choked peer "+ peer.peerId);
        host.getReceiveStateMap().put(peer.peerId, State.CHOKED);
    }

    public static void handleUnChokeMessage(Peer host, PeerInfo peer, DataOutputStream outputStream)  throws IOException{
        host.log(String.format("Peer %s is unchoked by %s.", host.getPeerId(), peer.peerId));
        host.getReceiveStateMap().put(peer.peerId, State.UNCHOKED);
        //send a request
        sendRequest(host, peer, outputStream);
    }

    public static void sendRequest(Peer host, PeerInfo peer, DataOutputStream outputStream) throws IOException{
        Integer nextPieceNeeded = host.findNextPieceNeeded(peer.peerId);

        if (nextPieceNeeded != null) {
            host.getRequestedPieces().add(nextPieceNeeded);
            // Send the request message for the next piece needed
            RequestMessage requestMessage = new RequestMessage(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(nextPieceNeeded).array());
            requestMessage.sendRequest(outputStream);
//            host.log(String.format("Peer %s sent request to %s for piece %s.", host.getPeerId(), peer.peerId, nextPieceNeeded));
        }

    }

    public static void handleRequestMessage(Peer host, PeerInfo peer, DataOutputStream outputStream, RequestMessage requestMessage) throws IOException {
        if (host.getSendStateMap().get(peer.peerId).equals(State.UNCHOKED)) { //check for choke or optimistic
            int pieceRequired = ByteBuffer.wrap(requestMessage.getRequestMessage()).order(ByteOrder.BIG_ENDIAN).getInt();
            byte[] pieceRequestedInBytes = requestMessage.getRequestMessage();
            byte[] payLoad = host.getFilePieces()[pieceRequired].getData();

            ByteBuffer messagePayload = ByteBuffer.allocate(pieceRequestedInBytes.length + payLoad.length);
            messagePayload.put(pieceRequestedInBytes, 0, pieceRequestedInBytes.length);
            messagePayload.put(payLoad, 0, payLoad.length);
            PieceMessage pieceMessage = new PieceMessage(messagePayload.array());
            pieceMessage.sendPieceMessage(outputStream);
           // senderSocketHandler.sendMessage(otherId, pieceMessage.getMessage());
//            host.log(String.format("Peer %s sent piece message to %s ", host.getPeerId(), peer.peerId));

        }
    }

    public static Integer handlePieceMessage(Peer host, PeerInfo peer, PieceMessage message) {
        byte[] messagePayload = message.getPieceMessage();

        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(messagePayload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
        BitField peerBitfield = host.getPeerBitfieldMap().get(peer.peerId);
//        if (peerBitfield.getBitFieldBit(pieceIndex) && !host.getBitField().getBitFieldBit(pieceIndex)) {
            byte[] piece = Arrays.copyOfRange(messagePayload, 4, messagePayload.length);
            host.getFilePieces()[pieceIndex] = new FilePiece(piece); // added piece to filePieces

            //Set bit to 1
            host.getBitField().setBitFieldBit(pieceIndex);

            host.log(String.format("Peer %s has downloaded the piece %d from %s. Now the number of pieces it has is %d",
                    host.getPeerId(), pieceIndex, peer.peerId, host.getBitField().getCardinality()));

            // If all are received, merge the file.
            if (host.getBitField().getCardinality() == host.getFilePieces().length) // checks if all bits are set
                try {
                    host.getFileUtil().constructFile(host);
                    host.setHasFile(true);
                    host.log(String.format("Peer %s has downloaded the complete file.", host.getPeerId()));
                    checkTermination(host);
                } catch (IOException e) {
                    System.out.println("Error in creating file");
                    e.printStackTrace();
                }
            return pieceIndex;
//        }
//        return null;
    }

    public static void checkTermination(Peer host) {
        for(Map.Entry<String, BitField> entry : host.getPeerBitfieldMap().entrySet()){
//                System.out.println(entry.getKey() + "Cardinality " + entry.getValue().getCardinality() + "Length " + host.getFilePieces().length);
            host.log(entry.getKey() + " bitfield " + entry.getValue().getCardinality() + " / " + host.getFilePieces().length);
            if(entry.getValue().getCardinality() != host.getFilePieces().length ){
                return;
            }
        }
        System.out.println("All peers have files");
        host.exit();
    }

    public static void handleHaveMessage(Peer host, PeerInfo peer, DataOutputStream outputStream, HaveMessage message) throws IOException {
        //System.out.println(host.getPeerId() +" received have from " +peer.peerId);
        int pieceIndex = message.getPieceIndex();
        host.log(String.format("Peer %s received the 'have' message from %s for the piece %d", host.getPeerId(), peer.peerId, pieceIndex));

        host.getPeerBitfieldMap().get(peer.peerId).getBitField().set(pieceIndex);
//        System.out.println("cardinality " + peer.peerId + " " + host.getPeerBitfieldMap().get(peer.peerId).getCardinality());
       // System.out.println(pieceIndex + " bitfield of peer" + peer.peerId + BitSet.valueOf(host.getPeerBitfieldMap().get(peer.peerId).getBitField().toByteArray()).toString());
        if(host.getHasFile()){
            checkTermination(host);
        }
        else {
            if (!host.getBitField().getBitFieldBit(pieceIndex)) {
                // send interested
                InterestedMessage interestedMessage = new InterestedMessage();
                interestedMessage.sendInterestedMessage(outputStream);
            }
        }
    }






}
