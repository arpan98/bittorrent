package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;

public class UnchokeMessage extends Message {
    public UnchokeMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.UNCHOKE.getValue();
        this.messagePayload = new byte[0];
    }

    public void sendUnChokeMessage(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }
}
