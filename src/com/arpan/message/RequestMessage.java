package com.arpan.message;

public class RequestMessage extends Message{

    public RequestMessage(byte[] bytes) {
        this.messageLength = bytes.length;
        this.messageType = MessageType.REQUEST.getValue();
        this.messagePayload = bytes;
    }

    public byte[] getRequestMessage() {
        return this.messagePayload;
    }
}
