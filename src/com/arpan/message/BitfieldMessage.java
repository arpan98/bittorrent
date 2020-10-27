package com.arpan.message;

public class BitfieldMessage extends Message {
    public BitfieldMessage(byte[] bytes) {
        this.messageLength = bytes.length;
        this.messageType = MessageType.BITFIELD.getValue();
        this.messagePayload = bytes;
    }

    public byte[] getBitfield() {
        return this.messagePayload;
    }
}
