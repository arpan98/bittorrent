package com.arpan.timertask;

import com.arpan.Peer;
import com.arpan.message.ChokeMessage;
import com.arpan.message.UnchokeMessage;
import com.arpan.model.PeerInfo;
import com.arpan.model.State;
import com.arpan.util.Config;

import java.util.*;
import java.util.stream.Collectors;

public class PreferredNeighborsTask extends TimerTask {

    private final Peer peer;
    private final Config config;
    private final Map<String, PeerInfo> peerInfoMap;

    public PreferredNeighborsTask(Peer peer, Config config,  Map<String, PeerInfo> peerInfoMap) {
        this.peer = peer;
        this.config = config;
        this.peerInfoMap = peerInfoMap;
    }

    @Override
    public void run() {
        List<String> interestedPeers = peer.getPeersInterestedInMe();
        if (interestedPeers.isEmpty() || interestedPeers.size()==0)
            return;

        if (peer.hasCompleteFile()) {
            setPreferredNeighbors(getRandomPeers(interestedPeers, config.getNumberOfPreferredNeighbors()));
        } else {
            Map<String, Float> peerDownloadRateMap = peer.getPeerDownloadRateMap();

            List<String> preferredNeighbors = peerDownloadRateMap.entrySet().stream()
                    .filter(entry -> interestedPeers.contains(entry.getKey()))          // Is peer interested
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))     // Sort by decreasing order of download rate
                    .limit(config.getNumberOfPreferredNeighbors())                      // Limit to number of preferred neighbors
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            setPreferredNeighbors(preferredNeighbors);
        }
    }

    public void setPreferredNeighbors(List<String> preferredNeighbors) {
        List<String> previouslyPreferredNeighbors = peer.getPreferredNeighbors();
        if(preferredNeighbors==null)
            return;
        if (!preferredNeighbors.isEmpty()) {
            String preferredNeighborsString = String.join(", ", preferredNeighbors);
            peer.log(String.format("Peer %s has the preferred neighbors %s.", peer.getPeerId(), preferredNeighborsString));
        }

        previouslyPreferredNeighbors.stream()
                .filter(neighbor -> !preferredNeighbors.contains(neighbor))
                .forEach(this::sendChoke);

        preferredNeighbors.stream()
                .filter(neighbor -> !previouslyPreferredNeighbors.contains(neighbor))
                .forEach(this::sendUnchoke);
    }
    private void sendUnchoke(String otherId) {
        UnchokeMessage unchokeMessage = new UnchokeMessage();
        try {
            unchokeMessage.sendUnChokeMessage(peerInfoMap.get(otherId).outstream);
            peer.log(String.format("Peer %s sending unchoke message to %s ",peer.getPeerId(), otherId));

        }
        catch(Exception e){
            e.printStackTrace();
        }
        peer.getSendStateMap().put(otherId, State.UNCHOKED);
        System.out.println(peer.getPeerId() + "Sent unchoke to " + otherId);
    }

    private List<String> getRandomPeers(List<String> peers, int numberOfPeers) {
        List<String> selectedPeers = new ArrayList<>();
        Random rand = new Random();
        if(peers.size()==0)
            return null;
        for (int i = 0; i < numberOfPeers; i++) {
            if(peers.size()==0)
                break;
            int randomIndex = Math.abs(rand.nextInt(peers.size()));
            selectedPeers.add(peers.get(randomIndex));
            peers.remove(randomIndex);
        }
        return selectedPeers;
    }

    private void sendChoke(String otherId){
        ChokeMessage chokeMessage = new ChokeMessage();
        try {
            chokeMessage.sendChokeMessage(peerInfoMap.get(otherId).outstream);
        }catch(Exception e){
            e.printStackTrace();
        }
        peer.getSendStateMap().put(otherId, State.CHOKED);
        peer.log(String.format("Peer %s sending choke message to %s ",peer.getPeerId(), otherId));
        //System.out.println(peer.getPeerId() + "Sent choke to " + otherId);
    }
}
