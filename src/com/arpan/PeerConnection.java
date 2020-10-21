package com.arpan;


import com.arpan.message.HandshakeMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PeerConnection {

    private ObjectInputStream in;	    //stream read from the socket
    private ObjectOutputStream out;     //stream write to the socket
    private Socket socket;
    private boolean isConnected, isHandshakeDone;
    private String peerId;

    public PeerConnection() {
    }

    public String waitForHandshake() {
        byte[] data = new byte[32];
        if (isHandshakeDone)
            return peerId;
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

    public void sendMessage(byte[] msg)
    {
        try{
            out.write(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    public PeerConnection(Socket socket) {
        this.socket = socket;
        this.isConnected = true;
        setupStreams();
    }

    public boolean connect(String hostName, int portNum) {
        try {
            this.socket = new Socket(hostName, portNum);
            this.isConnected = true;
            setupStreams();
        } catch (IOException e) {
            return false;
        }
        return true;
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
