package com.moulberry.flashback.vectors;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.flashback.state.KeyframeTrack;
import com.moulberry.flashback.keyframe.Keyframe;
import org.joml.Vector3d;

import java.util.*;

public class VectorHandleManager {

    public static List<VectorHandle> createHandlesForKeyframe(KeyframeTrack track, int selectedTick, int trackIndex) {
        List<Integer> allTicks = new ArrayList<>(track.keyframesByTick.keySet());
        Collections.sort(allTicks); // Make sure ticks are in order
        int idx = allTicks.indexOf(selectedTick);

        List<VectorHandle> handles = new ArrayList<>();
        Keyframe selected = track.keyframesByTick.get(selectedTick);
        if (selected == null) return handles;

        Vector3d selectedPos = selected.getPosition();
        if (selectedPos == null) return handles;

        // To previous (2 blocks toward previous keyframe, if it exists)
        if (idx > 0) {
            int prevTick = allTicks.get(idx - 1);
            Keyframe prev = track.keyframesByTick.get(prevTick);
            if (prev != null) {
                Vector3d prevPos = prev.getPosition();
                if (prevPos != null) {
                    Vector3d dir = new Vector3d(prevPos).sub(selectedPos);
                    if (dir.lengthSquared() > 0) {
                        dir.normalize();
                        Vector3d twoBlocksTowardPrev = new Vector3d(selectedPos).add(dir.mul(2.0));
                        handles.add(new VectorHandle(trackIndex, selectedTick, VectorHandle.Type.TO_PREVIOUS, selectedPos, twoBlocksTowardPrev));
                    }
                }
            }
        }

        // To next (2 blocks toward next keyframe, if it exists)
        if (idx < allTicks.size() - 1) {
            int nextTick = allTicks.get(idx + 1);
            Keyframe next = track.keyframesByTick.get(nextTick);
            if (next != null) {
                Vector3d nextPos = next.getPosition();
                if (nextPos != null) {
                    Vector3d dir = new Vector3d(nextPos).sub(selectedPos);
                    if (dir.lengthSquared() > 0) {
                        dir.normalize();
                        Vector3d twoBlocksTowardNext = new Vector3d(selectedPos).add(dir.mul(2.0));
                        handles.add(new VectorHandle(trackIndex, selectedTick, VectorHandle.Type.TO_NEXT, selectedPos, twoBlocksTowardNext));
                    }
                }
            }
        }

        // Print info about each handle we are generating
        for (VectorHandle handle : handles) {
            System.out.printf(
                    "Handle: Type=%s, From=(%.2f, %.2f, %.2f), To=(%.2f, %.2f, %.2f)%n",
                    handle.getType(),
                    handle.getFrom().x, handle.getFrom().y, handle.getFrom().z,
                    handle.getTo().x, handle.getTo().y, handle.getTo().z
            );
        }

        return handles;
    }
    public static void createAndRenderHandlesForKeyframe(
            KeyframeTrack track,
            int selectedTick,
            int trackIndex,
            PoseStack poseStack,
            BufferBuilder bufferBuilder
    ) {
        List<VectorHandle> handles = createHandlesForKeyframe(track, selectedTick, trackIndex);
        // Directly render the handles
        VectorRenderer.renderHandles(handles, poseStack, bufferBuilder);
    }

}