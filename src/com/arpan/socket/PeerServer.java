package com.arpan.socket;

import com.arpan.Peer;
import com.arpan.message.HandshakeMessage;
import com.arpan.message.Messenger;
import com.arpan.model.PeerInfo;
import com.arpan.timertask.OptimisticUnchokingTask;
import com.arpan.timertask.PreferredNeighborsTask;
import com.arpan.util.Config;
import com.arpan.util.MessageUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Handler;

public class PeerServer extends Thread{
    Peer host;
    Map<String, PeerInfo> peerInfoMap;
    Map<String, PeerInfo> connectedPeerMap;
    private PreferredNeighborsTask preferredNeighborsTask;
    private  OptimisticUnchokingTask optimisticUnchokingTask;
    public PeerServer(Peer host, Map<String, PeerInfo> peerInfoMap, Map<String, PeerInfo> connectedPeerMap){
        this.host = host;
        this.peerInfoMap = peerInfoMap;
        this.connectedPeerMap = connectedPeerMap;
    }

    @Override
    public void run() {

        ServerSocket server = null;
        try {
            // Start the schedulers.
//            host.log(String.format("Peer %s starting its server ", host.getPeerId()));
//            host.log(String.format("Peer %s starting timer tasks", host.getPeerId()));
            startTimerTasks(host.config);

            server = new ServerSocket(host.portNum);
            while (true) {
                new Handler(server.accept()).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void startTimerTasks(Config config) {
        preferredNeighborsTask = new PreferredNeighborsTask(host, config, connectedPeerMap);
        new Timer().schedule(preferredNeighborsTask, 0, config.getUnchokingInterval() * 1000);

        optimisticUnchokingTask = new OptimisticUnchokingTask(host, connectedPeerMap);
        new Timer().schedule(optimisticUnchokingTask, 0, config.getOptimisticUnchokingInterval() * 1000);
    }

    private class Handler extends Thread {
        DataInputStream inStream;
        DataOutputStream outStream;
        Socket socket;
        PeerInfo peer;

        Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {

                inStream = new DataInputStream(socket.getInputStream());
                outStream = new DataOutputStream(socket.getOutputStream());

                String peerId = MessageUtil.HandShakeAndResponse(inStream, outStream, host.getPeerId());

                for (String key : peerInfoMap.keySet()) {
                    if (peerId.equals(key)) {
                        this.peer = peerInfoMap.get(key);
                        break;
                    }
                }
                peer.outstream = outStream;
                peer.socket = socket;
                connectedPeerMap.put(peer.peerId, peer);
                host.log(String.format("Peer %s is connected from Peer %s ", host.getPeerId(), peer.peerId));

                MessageUtil.sendBitfield(outStream, host.getBitField().getBitField());
//                host.log(String.format("Peer %s sending bitfield to peer %s ", host.getPeerId(), peer.peerId));

//                host.log(String.format("Peer %s starting messenger for all other tasks", host.getPeerId()));

                Messenger messenger = new Messenger(host, peer, peerInfoMap, inStream, outStream);

                messenger.receiveMessages();
            } catch (Exception e){// | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void kill(){
        optimisticUnchokingTask.cancel();
        preferredNeighborsTask.cancel();
    }
}
