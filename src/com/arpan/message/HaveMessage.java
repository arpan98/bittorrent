package com.arpan.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HaveMessage extends Message {
    public HaveMessage(int pieceIndex) {
        this.messageLength = Integer.BYTES;
        this.messageType = MessageType.HAVE.getValue();
        this.messagePayload = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(pieceIndex).array();
    }
}
