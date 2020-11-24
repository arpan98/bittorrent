package com.arpan.timertask;

import com.arpan.Peer;

import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class OptimisticUnchokingTask extends TimerTask {

    private final Peer peer;

    public OptimisticUnchokingTask(Peer peer) {
        this.peer = peer;
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
            peer.setOptimisticallyUnchokedNeighbor(options.get(rand.nextInt(options.size())));
    }
}
