package com.arpan.message;

import com.arpan.util.ByteUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class HandshakeMessage implements MessageInterface {
    public static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";

    private static final byte[] ZERO_BITS = new byte[10];

    private String peerId;

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public HandshakeMessage(String peerId) {
        this.peerId = peerId;
    }
    public HandshakeMessage(){}
    @Override
    public byte[] getMessage() {
        return ByteUtils.concatByteArrays(HANDSHAKE_HEADER.getBytes(StandardCharsets.ISO_8859_1),
                ZERO_BITS, peerId.getBytes(StandardCharsets.ISO_8859_1));
    }

    public void sendHandshake(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }

    public boolean recvHandshake(DataInputStream in, String requiredPeerId){
        byte[] data = new byte[32];

        try {
            int count = in.read(data);
            if (count == 32) {
                String handshakeHeader = new String(data, 0, 18, StandardCharsets.ISO_8859_1);
                this.peerId = new String(data, 28, 4, StandardCharsets.ISO_8859_1);
                //System.out.println("Peer Id in handshake"+ this.peerId);
                if (handshakeHeader.equals(HandshakeMessage.HANDSHAKE_HEADER)) {
                    if(requiredPeerId == null || requiredPeerId.equals(this.peerId))
                        return true;
                }
            }
        } catch (SocketTimeoutException ignored) {
        } catch (IOException e) {
            e.printStackTrace();

        }
        return false;

    }



}
