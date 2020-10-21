package com.arpan.message;

public class BitfieldMessage extends Message {
    public BitfieldMessage(byte[] bytes) {
        System.out.println(this.messageLength);
        this.messageLength = bytes.length;
        this.messageType = MessageType.BITFIELD.getValue();
        this.messagePayload = bytes;
    }

    public byte[] getBitfield() {
        return this.messagePayload;
    }
}
