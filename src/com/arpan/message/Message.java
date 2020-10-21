package com.arpan.message;

import com.arpan.ByteUtils;

import java.nio.ByteBuffer;

public abstract class Message implements MessageInterface {
    protected int messageLength;
    protected byte messageType;
    protected byte[] messagePayload;

    @Override
    public byte[] getMessage() {
        return ByteUtils.concatByteArrays(ByteBuffer.allocate(4).putInt(messageLength).array(),
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
