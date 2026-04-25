package me.almana.wgsplicer.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class ExportChatBuilder {

    private static final Component PREFIX = Component.literal("[WGSplicer] ").withStyle(ChatFormatting.GOLD);

    private ExportChatBuilder() {}

    public static MutableComponent info(String message) {
        return Component.empty()
                .append(PREFIX)
                .append(Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    public static MutableComponent failure(String message) {
        return Component.empty()
                .append(PREFIX)
                .append(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static MutableComponent success(MinecraftServer server, int rows, Path json, String websiteUrl) {
        MutableComponent line = Component.empty()
                .append(PREFIX)
                .append(Component.literal("Export complete (" + rows + " rows)").withStyle(ChatFormatting.GREEN));

        if (json != null) {
            Path serverDir = server.getServerDirectory();
            line.append(Component.literal("\n  JSON: ").withStyle(ChatFormatting.GRAY))
                    .append(fileLink(serverDir, json))
                    .append(Component.literal("  "))
                    .append(websiteButton(websiteUrl));
        }

        return line;
    }

    private static MutableComponent fileLink(Path serverDir, Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        String displayed = relativizeForDisplay(serverDir, absolute);
        Style style = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, absolute.toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Open ").append(Component.literal(absolute.toString()).withStyle(ChatFormatting.WHITE))));
        return Component.literal(displayed).withStyle(style);
    }

    private static MutableComponent websiteButton(String url) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.LIGHT_PURPLE)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Open World Generation Splicer viewer\n").append(Component.literal(url).withStyle(ChatFormatting.WHITE))));
        return Component.literal("[Open Web]").withStyle(style);
    }

    private static String relativizeForDisplay(Path serverDir, Path absolute) {
        try {
            Path base = serverDir.toAbsolutePath().normalize();
            Path rel = base.relativize(absolute);
            String s = rel.toString().replace('\\', '/');
            if (s.startsWith("../")) return absolute.toString().replace('\\', '/');
            return s;
        } catch (IllegalArgumentException e) {
            return absolute.toString().replace('\\', '/');
        }
    }
}
