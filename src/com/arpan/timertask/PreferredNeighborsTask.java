package com.arpan.timertask;

import com.arpan.Peer;
import com.arpan.util.Config;

import java.util.*;
import java.util.stream.Collectors;

public class PreferredNeighborsTask extends TimerTask {

    private final Peer peer;
    private final Config config;

    public PreferredNeighborsTask(Peer peer, Config config) {
        this.peer = peer;
        this.config = config;
    }

    @Override
    public void run() {
        List<String> interestedPeers = peer.getPeersInterestedInMe();
        if (interestedPeers.isEmpty())
            return;

        if (peer.hasCompleteFile()) {
            peer.setPreferredNeighbors(getRandomPeers(interestedPeers, config.getNumberOfPreferredNeighbors()));
        } else {
            Map<String, Float> peerDownloadRateMap = peer.peerDownloadRateMap;

            List<String> preferredNeighbors = peerDownloadRateMap.entrySet().stream()
                    .filter(entry -> interestedPeers.contains(entry.getKey()))          // Is peer interested
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))     // Sort by decreasing order of download rate
                    .limit(config.getNumberOfPreferredNeighbors())                      // Limit to number of preferred neighbors
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            peer.setPreferredNeighbors(preferredNeighbors);
        }
    }

    private List<String> getRandomPeers(List<String> peers, int numberOfPeers) {
        List<String> selectedPeers = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < numberOfPeers; i++) {
            int randomIndex = rand.nextInt(peers.size());
            selectedPeers.add(peers.get(randomIndex));
            peers.remove(randomIndex);
        }
        return selectedPeers;
    }
}
