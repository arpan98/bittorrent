package com.arpan.message;

public class NotInterestedMessage extends Message {
    public NotInterestedMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.NOT_INTERESTED.getValue();
        this.messagePayload = new byte[0];
    }
}
