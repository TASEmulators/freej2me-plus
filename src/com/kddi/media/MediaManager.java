package com.kddi.media;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// This class manages resources related to MediaPlayer.
// This is not written in documentation, and should be moved to other package.
public class MediaManager {
    
    static Map<MediaResource, byte[]> resourceMap = new HashMap<MediaResource, byte[]>();
    static Map<MediaResource, Set<MediaPlayerBox>> playerBoxMap = new HashMap<MediaResource, Set<MediaPlayerBox>>();

    public static byte[] getResource(MediaResource resource) {
        return resourceMap.get(resource);
    }

    public static void putResource(MediaResource resource, byte[] data) {
        resourceMap.put(resource, data);
        playerBoxMap.put(resource, new HashSet<MediaPlayerBox>());
    }

    public static void removeResource(MediaResource resource) {
        resourceMap.remove(resource);
        playerBoxMap.remove(resource);
    }

    public static MediaPlayerBox[] getMediaPlayerBoxes(MediaResource resource) {
        return playerBoxMap.get(resource).toArray(new MediaPlayerBox[0]);
    }

    public static void linkMediaResourceToMediaPlayerBox(MediaResource resource, MediaPlayerBox box) 
    {
        playerBoxMap.putIfAbsent(resource, new HashSet<MediaPlayerBox>());
        Set<MediaPlayerBox> playerBoxSet = playerBoxMap.get(resource);
        playerBoxSet.add(box);
    }

    public static void unlinkMediaResource(MediaResource resource, MediaPlayerBox box) {
        var playerBoxSet = playerBoxMap.get(resource);
        if (playerBoxSet == null) {
            System.out.println("this resource is not registered");
            return;
        }
        if (playerBoxSet.contains(box)) {
            playerBoxSet.remove(box);
        } else {
            System.out.println("this resource is not linked to mediaPlayerBox");
        }
    } 
}
