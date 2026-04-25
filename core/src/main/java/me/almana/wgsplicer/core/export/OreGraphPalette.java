package me.almana.wgsplicer.core.export;

import me.almana.wgsplicer.core.domain.BlockId;

import java.awt.Color;

public final class OreGraphPalette {

    private static final Color[] FALLBACK = {
            new Color(0x4F, 0xC3, 0xF7), new Color(0xFF, 0xB7, 0x4D), new Color(0x81, 0xC7, 0x84),
            new Color(0xE5, 0x73, 0x73), new Color(0xBA, 0x68, 0xC8), new Color(0x64, 0xB5, 0xF6),
            new Color(0xFF, 0xD5, 0x4F), new Color(0xA1, 0x88, 0x7F), new Color(0x90, 0xA4, 0xAE),
            new Color(0x4D, 0xB6, 0xAC), new Color(0xF0, 0x62, 0x92), new Color(0xAE, 0xD5, 0x81)
    };

    private OreGraphPalette() {}

    public static Color color(BlockId id) {
        String path = id.path();
        if (path.contains("diamond")) return new Color(0x4D, 0xD0, 0xE1);
        if (path.contains("emerald")) return new Color(0x66, 0xBB, 0x6A);
        if (path.contains("netherite") || path.contains("ancient_debris")) return new Color(0x6D, 0x4C, 0x41);
        if (path.contains("gold")) return new Color(0xFF, 0xD5, 0x4F);
        if (path.contains("iron")) return new Color(0xD7, 0xCC, 0xC8);
        if (path.contains("redstone")) return new Color(0xE5, 0x39, 0x35);
        if (path.contains("lapis")) return new Color(0x1E, 0x88, 0xE5);
        if (path.contains("copper")) return new Color(0xFF, 0x70, 0x43);
        if (path.contains("coal")) return new Color(0x42, 0x42, 0x42);
        if (path.contains("quartz")) return new Color(0xB0, 0xBE, 0xC5);
        return FALLBACK[Math.floorMod(id.toString().hashCode(), FALLBACK.length)];
    }

    public static String nameLabel(BlockId id) {
        String path = id.path();
        if (path.endsWith("_ore")) path = path.substring(0, path.length() - 4);
        return path;
    }

    public static String hexColor(BlockId id) {
        Color c = color(id);
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
