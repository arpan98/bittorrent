package com.arpan;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Config {

    public static final String NumberOfPreferredNeighbors = "NumberOfPreferredNeighbors";
    public static final String UnchokingInterval = "UnchokingInterval";
    public static final String OptimisticUnchokingInterval = "OptimisticUnchokingInterval";
    public static final String FileName = "FileName";
    public static final String FileSize = "FileSize";
    public static final String PieceSize = "PieceSize";

    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private long fileSize;
    private int pieceSize;

    public void readCommon(String fileName) throws FileNotFoundException {
        File configFile = new File(fileName);
        Scanner sc = new Scanner(configFile);
        while (sc.hasNextLine()) {
            String data = sc.nextLine();
            readCommonConfigLine(data);
        }
    }

    // Returns true if this peer has the file
    public List<PeerInfo> readPeerInfo(String peerId, String fileName) throws FileNotFoundException {
        File configFile = new File(fileName);
        Scanner sc = new Scanner(configFile);
        List<PeerInfo> peerInfoList = new ArrayList<>();
        while (sc.hasNextLine()) {
            String data = sc.nextLine();
            peerInfoList.add(readPeerInfoConfigLine(peerId, data));
        }
        return peerInfoList;
    }

    public void readCommonConfigLine(String configLine) {
        String variableName = configLine.split(" ")[0];
        String value = configLine.split(" ")[1];
        switch (variableName) {
            case NumberOfPreferredNeighbors -> numberOfPreferredNeighbors = Integer.parseInt(value);
            case UnchokingInterval -> unchokingInterval = Integer.parseInt(value);
            case OptimisticUnchokingInterval -> optimisticUnchokingInterval = Integer.parseInt(value);
            case FileName -> fileName = value;
            case FileSize -> fileSize = Long.parseLong(value);
            case PieceSize -> pieceSize = Integer.parseInt(value);
        }
    }

    // Returns true if this peer has the file
    public PeerInfo readPeerInfoConfigLine(String peerId, String configLine) {

        PeerInfo peerInfo = new PeerInfo(configLine);

        return peerInfo;
    }

    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }
}
