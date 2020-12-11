package com.arpan.message;

import java.io.DataOutputStream;
import java.io.IOException;

public class RequestMessage extends Message{

    public RequestMessage(byte[] bytes) {
        this.messageLength = bytes.length;
        this.messageType = MessageType.REQUEST.getValue();
        this.messagePayload = bytes;
    }

    public byte[] getRequestMessage() {
        return this.messagePayload;
    }
    public void sendRequest(DataOutputStream out) throws IOException
    {
        out.write(getMessage());
        out.flush();
    }
}
