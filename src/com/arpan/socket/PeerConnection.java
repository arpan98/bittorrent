package com.arpan.socket;


import com.arpan.util.ByteUtils;
import com.arpan.message.HandshakeMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class PeerConnection {

    private static final int SO_TIMEOUT = 1000; // milliseconds

    private ObjectInputStream in;	    //stream read from the socket
    private ObjectOutputStream out;     //stream write to the socket
    private final Object inLock = new Object();
    private final Object outLock = new Object();
    private Socket socket;
    private boolean isHandshakeDone;
    private String peerId;

    private ReceiverSocketHandler receiverSocketHandler;

    public PeerConnection() {
    }

    public PeerConnection(Socket socket, ReceiverSocketHandler receiverSocketHandler) {
        this.socket = socket;
        try {
            this.socket.setSoTimeout(SO_TIMEOUT);
//            this.socket.setTcpNoDelay(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.receiverSocketHandler = receiverSocketHandler;
        setupStreams();
    }

    public String waitForHandshake() {
        byte[] data = new byte[32];
        if (isHandshakeDone)
            return peerId;
        synchronized (inLock) {
            while (true) {
                try {
                    int count = in.read(data);
                    if (count == 32) {
                        String handshakeHeader = new String(data, 0, 18, StandardCharsets.ISO_8859_1);
                        if (handshakeHeader.equals(HandshakeMessage.HANDSHAKE_HEADER)) {
                            isHandshakeDone = true;
                            peerId = getPeerFromHandshakeMessage(data);
                            return peerId;
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void tryReceive() {
        if (!isHandshakeDone)
            return;
//        System.out.println("Trying receive from " + peerId);
        synchronized (inLock) {
            byte[] lengthBytes = new byte[4];
            try {
                int count = in.read(lengthBytes);
                if (count != 4) {
                    if (count != -1)
                        System.out.println("Message length is " + count + " bytes, should be 4 bytes");
                    return;
                }
                int messageLength = ByteUtils.readMessageLength(lengthBytes);
                System.out.println("Incoming message length = " + messageLength);

                byte messageType = in.readByte();

                byte[] messagePayload = new byte[messageLength];
                count = 0;
                while (count != messageLength) {
                    count += in.read(messagePayload, count, messageLength-count);
//                    System.out.println("Bytes read = " + count + " Message length = " + messageLength);
                }
                System.out.println("Message type = " + messageType);
                ByteUtils.printBits(messagePayload);
                receiverSocketHandler.onReceivedMessage(peerId, messageType, messagePayload);

            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getPeerFromHandshakeMessage(byte[] bytes) {
        return new String(bytes,28, 4, StandardCharsets.ISO_8859_1);
    }

    public void sendHandshake(HandshakeMessage handshakeMessage) {
        sendMessage(handshakeMessage.getMessage());
    }

    public void sendMessage(byte[] msg) {
        synchronized (outLock) {
            try {
                out.write(msg);
                out.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public boolean connect(String hostName, int portNum) {
        try {
            this.socket = new Socket(hostName, portNum);
            this.socket.setSoTimeout(SO_TIMEOUT);
//            this.socket.setTcpNoDelay(true);
            setupStreams();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void setReceiverSocketHandler(ReceiverSocketHandler receiverSocketHandler) {
        this.receiverSocketHandler = receiverSocketHandler;
    }

    private void setupStreams() {
        try {
            this.out = new ObjectOutputStream(this.socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException e) {
            System.out.println("Socket disconnected");
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isHandshakeDone() {
        return isHandshakeDone;
    }

    public void setHandshakeDone(boolean handshakeDone) {
        this.isHandshakeDone = handshakeDone;
    }
}
