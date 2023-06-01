package de.uniks.beastopia.teaml.rest;

import java.util.List;

public record Layer(
        List<Chunk> chunks,
        String id,
        int opacity,
        int startx,
        int starty,
        String type,
        boolean visible,
        int height,
        int width,
        int x,
        int y
) {
}
