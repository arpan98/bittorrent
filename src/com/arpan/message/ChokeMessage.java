package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;

public class ChokeMessage extends Message {
    public ChokeMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.CHOKE.getValue();
        this.messagePayload = new byte[0];
    }

    public void sendChokeMessage(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }
}
