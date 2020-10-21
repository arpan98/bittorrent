package com.arpan.message;

public class ChokeMessage extends Message {
    public ChokeMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.CHOKE.getValue();
        this.messagePayload = new byte[0];
    }
}
