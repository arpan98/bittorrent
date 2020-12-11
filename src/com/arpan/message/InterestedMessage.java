package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;

public class InterestedMessage extends Message {
    public InterestedMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.INTERESTED.getValue();
        this.messagePayload = new byte[0];
    }
    public void sendInterestedMessage(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }
}
