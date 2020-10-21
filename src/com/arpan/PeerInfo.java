package com.arpan;

public class PeerInfo {
    String peerId;
    String hostName;
    int portNum;
    boolean hasFile;

    public PeerInfo(String configLine) {
        String[] splitLine = configLine.split(" ");
        this.peerId = splitLine[0];
        this.hostName = splitLine[1];
        this.portNum = Integer.parseInt(splitLine[2]);
        this.hasFile = splitLine[3].equals("1");
    }
}
