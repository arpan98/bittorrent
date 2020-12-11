package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;

public class BitfieldMessage extends Message {
    public BitfieldMessage(byte[] bytes) {
        super();
        this.messageLength = bytes.length;
        this.messageType = MessageType.BITFIELD.getValue();
        this.messagePayload = bytes;
    }

    public byte[] getBitfield() {
        return this.messagePayload;
    }

    public void sendBitfield(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }
}
