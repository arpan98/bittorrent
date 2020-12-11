package com.arpan.util;

import com.arpan.model.PeerInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

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

    public Map<String,PeerInfo> readPeerInfo(String fileName) throws FileNotFoundException {
        File configFile = new File(fileName);
        Scanner sc = new Scanner(configFile);
        Map<String, PeerInfo> peerInfoMap = new ConcurrentHashMap<>();
        while (sc.hasNextLine()) {
            String data = sc.nextLine();
            PeerInfo peer = readPeerInfoConfigLine(data);
            peerInfoMap.put(peer.peerId, peer);
        }
        return peerInfoMap;
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

    public PeerInfo readPeerInfoConfigLine(String configLine) {
        return new PeerInfo(configLine);
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
