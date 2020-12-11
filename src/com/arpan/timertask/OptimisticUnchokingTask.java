package com.arpan.timertask;

import com.arpan.Peer;
import com.arpan.message.UnchokeMessage;
import com.arpan.model.PeerInfo;
import com.arpan.model.State;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class OptimisticUnchokingTask extends TimerTask {

    private final Peer peer;
    private final Map<String, PeerInfo> peerInfoMap;
    public OptimisticUnchokingTask(Peer peer, Map<String, PeerInfo> peerInfoMap) {
        this.peer = peer;
        this.peerInfoMap  = peerInfoMap;
    }

    @Override
    public void run() {
        List<String> interestedPeers = peer.getPeersInterestedInMe();
        if (interestedPeers.isEmpty())
            return;

        List<String> options = peer.getSendChokedPeers().stream()
                .filter(interestedPeers::contains)
                .collect(Collectors.toList());

        Random rand = new Random();
        if (options.size() > 0)
            setOptimisticallyUnchokedNeighbor(options.get(rand.nextInt(options.size())));
    }

    public void setOptimisticallyUnchokedNeighbor(String otherId) {
        peer.log(String.format("Peer %s has the optimistically unchoked neighbor %s.", peer.getPeerId(), otherId));
        sendUnchoke(otherId);
    }

    private void sendUnchoke(String otherId) {
        UnchokeMessage unchokeMessage = new UnchokeMessage();
        try {
            unchokeMessage.sendUnChokeMessage(peerInfoMap.get(otherId).outstream);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        //senderSocketHandler.sendMessage(otherId, unchokeMessage.getMessage());
        peer.getSendStateMap().put(otherId, State.UNCHOKED);
        System.out.println(peer.getPeerId() + "Sent unchoke to " + otherId);
    }


}
