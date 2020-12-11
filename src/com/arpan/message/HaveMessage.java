package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HaveMessage extends Message {
    public HaveMessage(int pieceIndex) {
        this.messageLength = Integer.BYTES;
        this.messageType = MessageType.HAVE.getValue();
        this.messagePayload = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(pieceIndex).array();
    }

    public HaveMessage(byte[] bytes) {
        this.messageLength = bytes.length;
        this.messageType = MessageType.HAVE.getValue();
        this.messagePayload = bytes;
    }

    public int getPieceIndex() {
        return ByteBuffer.wrap(messagePayload).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public void sendHaveMessage(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }
}
