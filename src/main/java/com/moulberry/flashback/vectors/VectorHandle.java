package com.moulberry.flashback.vectors;

import org.joml.Vector3d;

public class VectorHandle {
    public enum Type { TO_PREVIOUS, TO_NEXT }

    private final int trackIndex;
    private final int tick;
    private final Type type;
    private final Vector3d from;
    private final Vector3d to;
    private boolean hovered;
    private boolean selected;

    public VectorHandle(int trackIndex, int tick, Type type, Vector3d from, Vector3d to) {
        this.trackIndex = trackIndex;
        this.tick = tick;
        this.type = type;
        this.from = from;
        this.to = to;
        this.hovered = false;
        this.selected = false;
    }

    public int getTrackIndex() { return trackIndex; }
    public int getTick() { return tick; }
    public Type getType() { return type; }
    public Vector3d getFrom() { return from; }
    public Vector3d getTo() { return to; }
    public boolean isHovered() { return hovered; }
    public void setHovered(boolean hovered) { this.hovered = hovered; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}