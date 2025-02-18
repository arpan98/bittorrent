package com.arpan.message;

import com.arpan.util.ByteUtils;

public class Message implements MessageInterface {
    protected int messageLength;
    protected byte messageType;
    protected byte[] messagePayload;
    public Message(){}
    public Message(byte messageType, byte[] messagePayload) {
        this.messageType = messageType;
        this.messagePayload = messagePayload;
    }

    @Override
    public byte[] getMessage() {
        return ByteUtils.concatByteArrays(ByteUtils.messageLengthToBytes(messageLength),
                new byte[] {messageType}, messagePayload);
    }

    public int getMessageLength() {
        return messageLength;
    }

    public byte getMessageType() {
        return messageType;
    }

    public byte[] getMessagePayload() {
        return messagePayload;
    }

}
