package com.arpan.socket;

import com.arpan.Peer;
import com.arpan.message.HandshakeMessage;
import com.arpan.message.Messenger;
import com.arpan.model.PeerInfo;
import com.arpan.util.MessageUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class PeerClient extends Thread{
    Peer host;
    Map<String, PeerInfo> peerInfoMap;
    Map<String, PeerInfo> connectedPeerMap;

    public PeerClient(Peer host, Map<String, PeerInfo> peerInfoMap, Map<String, PeerInfo> connectedPeerMap){
        this.host = host;
        this.peerInfoMap = peerInfoMap;
        this.connectedPeerMap = connectedPeerMap;
    }

    @Override
    public void run() {
        // Send handshake and bitfield requests to the peers above me
        for (String key : peerInfoMap.keySet()) {
                if(Integer.parseInt(key) < Integer.parseInt(host.getPeerId()))
                    new Client(peerInfoMap.get(key)).start();

        }
    }

    private class Client extends Thread {
        DataInputStream inStream;
        DataOutputStream outStream;
        Socket socket;
        PeerInfo peer;

        Client(PeerInfo peerInfo) {
            this.peer = peerInfo;
        }

        @Override
        public void run() {
            try {
                System.out.println(peer.hostName +" "+ peer.portNum);
                socket = new Socket(peer.hostName, peer.portNum);
                inStream = new DataInputStream(socket.getInputStream());
                outStream = new DataOutputStream(socket.getOutputStream());
                peer.socket = socket;
                peer.outstream = outStream;
                connectedPeerMap.put(peer.peerId, peer);
                host.log("Peer " + host.getPeerId() + " makes a connection to Peer " + peer.peerId);
                //Send handshake,validate it and send bitfield
                HandshakeMessage handshakeMsg = new HandshakeMessage(host.getPeerId());
                handshakeMsg.sendHandshake(outStream);
                HandshakeMessage recvdHandshake = new HandshakeMessage();
                while (true) {
                    if(recvdHandshake.recvHandshake(inStream, peer.peerId)) {
//                        host.log("Handshake received from"+ peer.peerId);
                        break;

                    }
//                    host.log("rrect handshake received");
                }
                peerInfoMap.put(peer.peerId, peer);

                //Sending bitfield message
//                host.log("Bitfield being sent" + peer.peerId +" bitfield:::" + host.getBitField().toByteArray());
                MessageUtil.sendBitfield(outStream, host.getBitField().getBitField());

                //Initalphase done, now the client waits for the messages
                Messenger msgService = new Messenger(host, peer, connectedPeerMap, inStream, outStream);
                msgService.receiveMessages();

            } catch (IOException  | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
