package com.moulberry.flashback.visuals;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.windows.TimelineWindow;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.record.ReplayMarker;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.vectors.VectorHandle;
import com.moulberry.flashback.vectors.VectorRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.List;

public class WorldRenderHook {

    private static VertexBuffer debugLineVertexBuffer = null;
    private static VertexBuffer handlesVertexBuffer = null;
    private static List<VectorHandle> lastHandles = null;

    public static void renderHook(PoseStack poseStack, float partialTick, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f projection) {
        ReplayServer replayServer = Flashback.getReplayServer();
        if (replayServer == null || Flashback.isExporting()) return;

        // CameraPath rendering (no change)
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState != null && editorState.replayVisuals.cameraPath) {
            CameraPath.renderCameraPath(poseStack, camera, replayServer);
        }

        // --- Handles rendering using VertexBuffer (CameraPath style) ---
        List<VectorHandle> handles = TimelineWindow.currentHandles;
        if (handles != null && !handles.isEmpty()) {
            // Only rebuild if handles changed
            if (lastHandles == null || !lastHandles.equals(handles)) {
                if (handlesVertexBuffer != null) {
                    handlesVertexBuffer.close();
                    handlesVertexBuffer = null;
                }
                BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                VectorRenderer.renderHandles(handles, poseStack, bufferBuilder);
                MeshData meshData = bufferBuilder.build();
                if (meshData != null) {
                    handlesVertexBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
                    handlesVertexBuffer.bind();
                    handlesVertexBuffer.upload(meshData);
                    VertexBuffer.unbind();
                }
                lastHandles = handles;
            }

            if (handlesVertexBuffer != null) {
                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.lineWidth(2f);
                var shaderInstance = RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);

                poseStack.pushPose();
                poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
                handlesVertexBuffer.bind();
                handlesVertexBuffer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shaderInstance);
                VertexBuffer.unbind();
                poseStack.popPose();
            }
        } else {
            // Clean up VertexBuffer if no handles
            if (handlesVertexBuffer != null) {
                handlesVertexBuffer.close();
                handlesVertexBuffer = null;
            }
            lastHandles = null;
        }

        // --- Persistent hardcoded green debug line ---
        ensureDebugLineVertexBuffer();
        if (debugLineVertexBuffer != null) {
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(2f);
            var shaderInstance = RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);

            poseStack.pushPose();
            poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
            debugLineVertexBuffer.bind();
            debugLineVertexBuffer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shaderInstance);
            VertexBuffer.unbind();
            poseStack.popPose();
        }

        // --- Replay marker rendering (unchanged) ---
        FlashbackMeta meta = replayServer.getMetadata();
        if (!meta.replayMarkers.isEmpty()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            var multiBufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

            String dimension = Minecraft.getInstance().level.dimension().toString();
            multiBufferSource.endBatch();

            for (ReplayMarker marker : meta.replayMarkers.values()) {
                if (marker.position() == null) continue;
                ReplayMarker.MarkerPosition position = marker.position();
                if (!position.dimension().equals(dimension)) continue;

                poseStack.pushPose();
                poseStack.translate(
                        position.position().x - camera.getPosition().x,
                        position.position().y - camera.getPosition().y,
                        position.position().z - camera.getPosition().z
                );
                poseStack.mulPose(camera.rotation());

                final float width = 0.2f;
                bufferBuilder.addVertex(poseStack.last(), -width, -width, 0.0f).setUv(0f, 0f).setColor(marker.colour() | 0xFF000000);
                bufferBuilder.addVertex(poseStack.last(), width, -width, 0.0f).setUv(1f, 0f).setColor(marker.colour() | 0xFF000000);
                bufferBuilder.addVertex(poseStack.last(), width, width, 0.0f).setUv(1f, 1f).setColor(marker.colour() | 0xFF000000);
                bufferBuilder.addVertex(poseStack.last(), -width, width, 0.0f).setUv(0f, 1f).setColor(marker.colour() | 0xFF000000);

                if (marker.description() != null) {
                    Font font = Minecraft.getInstance().font;
                    Matrix4f matrix4f = poseStack.last().pose();
                    matrix4f.rotate((float) Math.PI, 0.0f, 1.0f, 0.0f);
                    matrix4f.scale(-0.025f, -0.025f, -0.025f);
                    int descriptionWidth = font.width(marker.description());
                    font.drawInBatch(marker.description(), -descriptionWidth / 2f, -20f, 0xFFFFFFFF,
                            true, matrix4f, multiBufferSource, Font.DisplayMode.NORMAL, 0, 0xF000F0);
                }
                poseStack.popPose();
            }

            MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
                RenderSystem.setShaderTexture(0, ResourceLocation.parse("flashback:world_marker_circle.png"));
                BufferUploader.drawWithShader(meshData);
            }
            multiBufferSource.endBatch();
        }
    }

    private static void ensureDebugLineVertexBuffer() {
        if (debugLineVertexBuffer != null) return;
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        bufferBuilder.addVertex(-200f, 90f, -200f).setColor(0f, 1f, 0f, 1f).setNormal(0f, 1f, 0f);
        bufferBuilder.addVertex(-190f, 90f, -200f).setColor(0f, 1f, 0f, 1f).setNormal(0f, 1f, 0f);
        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            debugLineVertexBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
            debugLineVertexBuffer.bind();
            debugLineVertexBuffer.upload(meshData);
            VertexBuffer.unbind();
        }
    }
}