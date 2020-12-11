package com.arpan.message;

import com.arpan.Peer;
import com.arpan.model.PeerInfo;
import com.arpan.util.ByteUtils;
import com.arpan.util.MessageUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class Messenger {

    Peer host;
    PeerInfo peer;
    Map<String, PeerInfo> peerInfoMap;
    DataInputStream inStream;
    DataOutputStream outStream;

    public Messenger(Peer host, PeerInfo peer, Map<String, PeerInfo> peerInfoMap, DataInputStream inStream,
            DataOutputStream outStream) {
        this.host = host;
        this.peer = peer;
        this.peerInfoMap = peerInfoMap;
        this.inStream = inStream;
        this.outStream = outStream;
        //System.out.println("Messenger for host "+ host.getPeerId() );
    }

    public void receiveMessages() {

        Message message;
        while(true) {
            try {
                message = getMessage(inStream);
                //System.out.println("Message Type is :::" + message.getMessageType());
                if (message.getMessageType() == MessageType.BITFIELD.getValue()) {
                    //System.out.println("Host " + host.getPeerId() + " received bitfield from" + peer.peerId);
                    BitfieldMessage bitfieldMessage = new BitfieldMessage(message.getMessagePayload());
                    MessageUtil.handleBitfieldMessage(host, peer, bitfieldMessage, outStream);
                }
                else if (message.getMessageType() == MessageType.INTERESTED.getValue()) {
                    MessageUtil.handleInterestedMessage(host, peer, outStream);
                }
                else if (message.getMessageType() == MessageType.NOT_INTERESTED.getValue()) {
                    MessageUtil.handleNotInterestedMessage(host, peer, outStream);
                }
                else if (message.getMessageType() == MessageType.CHOKE.getValue()) {
                    MessageUtil.handleChokeMessage(host, peer, outStream);
                }
                else if (message.getMessageType() == MessageType.UNCHOKE.getValue()) {
                    //System.out.println(host.getPeerId() + "Got unchoke message from " + peer.peerId);
                    MessageUtil.handleUnChokeMessage(host, peer, outStream);
                }
                else if (message.getMessageType() == MessageType.REQUEST.getValue()) {
                    RequestMessage requestMessage = new RequestMessage(message.getMessagePayload());
                    MessageUtil.handleRequestMessage(host, peer, outStream, requestMessage);
                    //System.out.println(host.getPeerId() + " Received request from " + peer.peerId);
                }
                else if (message.getMessageType() == MessageType.PIECE.getValue()) {
                    PieceMessage pieceMessage = new PieceMessage(message.getMessagePayload());
                    //System.out.println(host.getPeerId() + " received piece from " + peer.peerId);
                    Integer pieceIndex = MessageUtil.handlePieceMessage(host, peer, pieceMessage);
                    if(pieceIndex!=null){
                        //ie piece is received and set
                        //send have to all
                        HaveMessage haveMessage = new HaveMessage(pieceIndex);
                        for (PeerInfo connectedPeer : peerInfoMap.values()) {
                            haveMessage.sendHaveMessage(connectedPeer.outstream);
                            //System.out.println(host.getPeerId() + " Sent have [" + pieceIndex + "] message to " + connectedPeer);
                        }
                        //send request for other pieces
                        if(!host.getHasFile()){
                            MessageUtil.sendRequest(host,peer,outStream);
                        }

                    }
                }
                else if (message.getMessageType() == MessageType.HAVE.getValue()) {
                    HaveMessage haveMessage  = new HaveMessage(message.getMessagePayload());
                    MessageUtil.handleHaveMessage(host, peer, outStream, haveMessage);

                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    private Message getMessage(DataInputStream inStream) throws IOException {
        byte[] lengthBytes = new byte[4];
        try {
            int count = inStream.read(lengthBytes);
            if (count != 4) {
                if (count != -1)
                return null;
            }
            int messageLength = ByteUtils.readMessageLength(lengthBytes);

            byte messageType = inStream.readByte();
            long startTime  =System.nanoTime();
            byte[] messagePayload = new byte[messageLength];
            count = 0;
            while (count != messageLength) {
                count += inStream.read(messagePayload, count, messageLength-count);
            }
            long endTime = System.nanoTime();
            ByteUtils.printBits(messagePayload);

            if(messageType == MessageType.PIECE.getValue()) {
                peer.downLoadSpeed = messageLength / (endTime - startTime);
                host.getPeerDownloadRateMap().put(peer.peerId, peer.downLoadSpeed);
            }
            return new Message(messageType, messagePayload);

        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
