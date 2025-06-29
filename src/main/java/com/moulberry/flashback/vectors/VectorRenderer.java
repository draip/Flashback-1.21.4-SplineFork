package com.moulberry.flashback.vectors;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3d;
import java.util.List;

public class VectorRenderer {

    public static void renderHandles(List<VectorHandle> handles, PoseStack poseStack, BufferBuilder bufferBuilder) {
        if (handles == null || handles.isEmpty()) return;

        float size = 0.4f; // Cube size (adjust as desired)
        float yOffset = 1.62f; // Player eye height offset

        for (VectorHandle handle : handles) {
            Vector3d to = handle.getTo();
            float cx = (float) to.x;
            float cy = (float) to.y + yOffset;
            float cz = (float) to.z;

            float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;
            switch (handle.getType()) {
                case TO_PREVIOUS -> { r = 1.0f; g = 0.2f; b = 0.2f; }
                case TO_NEXT -> { r = 0.2f; g = 0.2f; b = 1.0f; }
            }
            drawWireCube(bufferBuilder, cx, cy, cz, size, r, g, b, a);
        }
    }

    private static void drawWireCube(BufferBuilder bufferBuilder, float cx, float cy, float cz, float size, float r, float g, float b, float a) {
        float h = size / 2f;
        float[][] v = {
                {cx-h, cy-h, cz-h},
                {cx+h, cy-h, cz-h},
                {cx+h, cy+h, cz-h},
                {cx-h, cy+h, cz-h},
                {cx-h, cy-h, cz+h},
                {cx+h, cy-h, cz+h},
                {cx+h, cy+h, cz+h},
                {cx-h, cy+h, cz+h}
        };
        int[][] e = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };
        for (int[] edge : e) {
            float[] p1 = v[edge[0]];
            float[] p2 = v[edge[1]];
            bufferBuilder.addVertex(p1[0], p1[1], p1[2])
                    .setColor(r, g, b, a)
                    .setNormal(0, 1, 0);
            bufferBuilder.addVertex(p2[0], p2[1], p2[2])
                    .setColor(r, g, b, a)
                    .setNormal(0, 1, 0);
        }
    }
}