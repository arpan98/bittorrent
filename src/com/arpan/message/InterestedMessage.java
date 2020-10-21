package com.arpan.message;

public class InterestedMessage extends Message {
    public InterestedMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.INTERESTED.getValue();
        this.messagePayload = new byte[0];
    }
}
