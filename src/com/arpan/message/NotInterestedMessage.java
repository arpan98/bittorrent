package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;

public class NotInterestedMessage extends Message {
    public NotInterestedMessage() {
        this.messageLength = 0;
        this.messageType = MessageType.NOT_INTERESTED.getValue();
        this.messagePayload = new byte[0];
    }

    public void sendNotInterestedMessage(DataOutputStream out) throws IOException {
        out.write(getMessage());
        out.flush();
    }
}
