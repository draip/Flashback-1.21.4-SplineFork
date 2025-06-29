package com.moulberry.flashback.timings;

import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.KeyframeTrack;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.types.CameraKeyframeType; // <-- correct import
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.ImDrawList;

import java.util.Map;

public class SplineWindow {
    private static boolean wasDocked = false;

    public static void render(ImBoolean open, boolean newlyOpened) {
        try {
            if (newlyOpened) {
                ImGuiViewport viewport = ImGui.getMainViewport();
                ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Appearing, 0.5f, 0.5f);
            }

            // Make the window 1.5x bigger
            ImGui.setNextWindowSizeConstraints(1050, 750, 5000, 5000);
            int flags = ImGuiWindowFlags.NoFocusOnAppearing;
            if (!wasDocked) {
                flags |= ImGuiWindowFlags.AlwaysAutoResize;
            }
            if (ImGui.begin("Spline Editor###SplineEditor", open, flags)) {
                wasDocked = ImGui.isWindowDocked();

                EditorState editorState = EditorStateManager.getCurrent();
                if (editorState == null) {
                    ImGui.text("No editor state available.");
                    ImGui.end();
                    return;
                }

                var startEnd = editorState.getExportStartAndEnd();
                int startTick = startEnd.start();
                int endTick = startEnd.end();

                // Check for invalid tick range
                if (startTick < 0 || endTick < 0) {
                    ImGui.text("Invalid tick values: start=" + startTick + " end=" + endTick);
                    ImGui.end();
                    return;
                }
                if (endTick <= startTick) {
                    ImGui.text("Not enough ticks to draw graph! (startTick=" + startTick + ", endTick=" + endTick + ")");
                    ImGui.end();
                    return;
                }

                // --- Prepare the graph area ---
                float canvasWidth = ImGui.getContentRegionAvailX();
                float canvasHeight = ImGui.getContentRegionAvailY() - 20;

                if (canvasWidth < 50) {
                    ImGui.text("Canvas width too small for graph: " + canvasWidth);
                    ImGui.end();
                    return;
                }
                if (canvasHeight < 50) {
                    ImGui.text("Canvas height too small for graph: " + canvasHeight);
                    ImGui.end();
                    return;
                }

                ImGui.beginChild("GraphRegion", canvasWidth, canvasHeight, true);

                float p0x = ImGui.getCursorScreenPosX();
                float p0y = ImGui.getCursorScreenPosY();
                float p1x = p0x + canvasWidth;
                float p1y = p0y + canvasHeight;

                ImDrawList drawList = ImGui.getWindowDrawList();

                // Use float versions for color to avoid native crash
                int bgCol    = ImGui.getColorU32(0.133f, 0.133f, 0.133f, 1.0f); // ~0xFF222222
                int axisCol  = ImGui.getColorU32(1.0f,   1.0f,   1.0f,   1.0f); // White
                int gridCol  = ImGui.getColorU32(1.0f,   1.0f,   1.0f,   0.13f); // White, alpha
                int curveCol = ImGui.getColorU32(0.0f,   1.0f,   0.0f,   1.0f); // Green
                int camCol   = ImGui.getColorU32(1.0f,   1.0f,   0.0f,   1.0f); // Yellow

                // Draw background
                drawList.addRectFilled(p0x, p0y, p1x, p1y, bgCol);

                // Draw axes
                // X axis (bottom)
                drawList.addLine(p0x, p1y, p1x, p1y, axisCol, 2.0f);
                // Y axis (left)
                drawList.addLine(p0x, p0y, p0x, p1y, axisCol, 2.0f);

                // Axis labels
                ImGui.setCursorScreenPos(p0x, p1y + 2);
                ImGui.text("Ticks (" + startTick + " to " + endTick + ")");
                ImGui.setCursorScreenPos(p0x - 32, p0y - 2);
                ImGui.text("Value (0-10)");

                // Draw vertical and horizontal grid lines (optional)
                for (int i = 1; i < 10; ++i) {
                    float y = p0y + (canvasHeight * i / 10f);
                    drawList.addLine(p0x, y, p1x, y, gridCol);
                }
                int tickCount = endTick - startTick;
                if (tickCount < 1) tickCount = 1; // guard

                for (int i = 1; i < tickCount; ++i) {
                    float x = p0x + (canvasWidth * i / (float)(tickCount));
                    drawList.addLine(x, p0y, x, p1y, gridCol);
                }

                // Draw a dummy example curve (replace with real data later)
                int pointCount = tickCount + 1;
                if (pointCount > 1) {
                    float prevX = -1, prevY = -1;
                    for (int i = 0; i < pointCount; ++i) {
                        float t = i / (float)(pointCount - 1);
                        int tick = startTick + Math.round(t * (endTick - startTick));
                        float value = 5 + 5 * (float)Math.sin(t * Math.PI * 2); // dummy curve from 0 to 10

                        float x = p0x + (tick - startTick) * canvasWidth / (endTick - startTick == 0 ? 1 : endTick - startTick);
                        float y = p1y - (value / 10f) * canvasHeight;
                        if (i > 0) {
                            drawList.addLine(prevX, prevY, x, y, curveCol, 2.0f);
                        }
                        drawList.addCircleFilled(x, y, 3, curveCol);
                        prevX = x; prevY = y;
                    }
                } else {
                    ImGui.text("Not enough points for curve. (pointCount=" + pointCount + ")");
                }

                // --- Draw camera keyframes as yellow points at Y=0.5 ---
                long stamp = editorState.acquireRead();
                try {
                    EditorScene scene = editorState.getCurrentScene(stamp);
                    for (KeyframeTrack track : scene.keyframeTracks) {
                        if (track.keyframeType == CameraKeyframeType.INSTANCE) {
                            for (Map.Entry<Integer, Keyframe> entry : track.keyframesByTick.entrySet()) {
                                int tick = entry.getKey();
                                if (tick < startTick || tick > endTick) continue;
                                float x = p0x + (tick - startTick) * canvasWidth / (endTick - startTick == 0 ? 1 : endTick - startTick);
                                float y = p1y - 0.5f * canvasHeight;
                                drawList.addCircleFilled(x, y, 8, camCol); // radius 8 for camera keyframes
                            }
                        }
                    }
                } finally {
                    editorState.release(stamp);
                }

                ImGui.endChild();
            }
            ImGui.end();
        } catch (Exception e) {
            ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "Exception in SplineWindow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}