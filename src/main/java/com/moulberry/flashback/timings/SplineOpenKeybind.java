package com.moulberry.flashback.timings;

import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class SplineOpenKeybind {

    private static KeyMapping openSplineGraphKeybind;

    public static void register() {
        openSplineGraphKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.flashback.timings.open_graph",
                GLFW.GLFW_KEY_N,
                "key.categories.flashback"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSplineGraphKeybind.consumeClick()) {
                if (isInFlashbackRecording()) {
                    if (!WindowOpenStateSpline.SPLINE.get()) {
                        WindowOpenStateSpline.splineNewlyOpened = true;
                    }
                    WindowOpenStateSpline.SPLINE.set(!WindowOpenStateSpline.SPLINE.get());
                }
            }
        });
    }

    private static boolean isInFlashbackRecording() {
        EditorState editorState = EditorStateManager.getCurrent();
        return editorState != null;
    }
}