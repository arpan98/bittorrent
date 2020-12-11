package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;

public class PieceMessage extends Message{
    public PieceMessage(byte[] bytes) {
        this.messageLength = bytes.length;
        this.messageType = MessageType.PIECE.getValue();
        this.messagePayload = bytes;
    }

    public byte[] getPieceMessage() {
        return this.messagePayload;
    }

    public void sendPieceMessage(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }
}
