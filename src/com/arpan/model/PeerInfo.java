package com.arpan.model;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.BitSet;

public class PeerInfo {
    public String peerId;
    public String hostName;
    public int portNum;
    public boolean hasFile;
    public DataOutputStream outstream;
    public Socket socket;

    public boolean isChoked = true;
    public boolean isOptimisticallyUnchoked = false;
    public boolean isInterested = false;

    public float downLoadSpeed;
    private BitSet requestedBitSet;

    public PeerInfo(String configLine) {
        String[] splitLine = configLine.split(" ");
        this.peerId = splitLine[0];
        this.hostName = splitLine[1];
        this.portNum = Integer.parseInt(splitLine[2]);
        this.hasFile = splitLine[3].equals("1");
        this.downLoadSpeed = 0;
    }
}
