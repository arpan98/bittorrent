package com.arpan.message;

import com.arpan.util.ByteUtils;

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

    @Override
    public byte[] getMessage() {
        return ByteUtils.concatByteArrays(HANDSHAKE_HEADER.getBytes(StandardCharsets.ISO_8859_1),
                ZERO_BITS, peerId.getBytes(StandardCharsets.ISO_8859_1));
    }
}
