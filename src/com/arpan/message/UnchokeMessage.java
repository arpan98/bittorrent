package com.arpan.message;

public class UnchokeMessage extends Message {
    public UnchokeMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.UNCHOKE.getValue();
        this.messagePayload = new byte[0];
    }
}
