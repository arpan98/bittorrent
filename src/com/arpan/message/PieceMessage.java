package com.arpan.message;

public class PieceMessage extends Message{
    public PieceMessage(byte[] bytes) {
        this.messageLength = bytes.length;
        this.messageType = MessageType.PIECE.getValue();
        this.messagePayload = bytes;
    }

    public byte[] getPieceMessage() {
        return this.messagePayload;
    }
}
