package com.example.statushud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class StatusHudClient implements ClientModInitializer {
    private static final int PANEL_COLOR = 0xA010141C;
    private static final int TEXT_COLOR = 0xFFE8EEF7;
    private static final int ACCENT_COLOR = 0xFF7DD3FC;
    private static final int LINE_HEIGHT = 11;

    private static KeyBinding toggleKey;
    private static boolean visible = true;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.statushud.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.statushud"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                visible = !visible;
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (visible) {
                renderHud(drawContext);
            }
        });
    }

    private static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }

        BlockPos position = player.getBlockPos();
        int ping = getPing(client);
        int fps = client.getCurrentFps();
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_048_576L;
        long allocatedMemory = Runtime.getRuntime().maxMemory() / 1_048_576L;
        long time = client.world.getTimeOfDay() % 24_000L;
        int hours = (int) ((time / 1_000L + 6L) % 24L);
        int minutes = (int) ((time % 1_000L) * 60L / 1_000L);
        String biome = client.world.getBiome(position).getKey()
                .map(key -> key.getValue().getPath())
                .orElse("unknown");
        String dimension = client.world.getRegistryKey().getValue().getPath();

        String[] lines = {
                "FPS  " + fps,
                "PING " + ping + " ms",
                "RAM  " + usedMemory + " / " + allocatedMemory + " MB",
                "XYZ  " + formatCoordinate(player.getX()) + " " + formatCoordinate(player.getY()) + " " + formatCoordinate(player.getZ()),
                "FACING " + player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT),
                "TIME " + String.format(Locale.ROOT, "%02d:%02d", hours, minutes),
                "BIOME " + biome,
                "LIGHT " + client.world.getLightLevel(position),
                "WORLD " + dimension
        };

        int width = 154;
        int height = lines.length * LINE_HEIGHT + 12;
        int x = 6;
        int y = 6;
        context.fill(x, y, x + width, y + height, PANEL_COLOR);
        context.fill(x, y, x + 2, y + height, ACCENT_COLOR);

        for (int index = 0; index < lines.length; index++) {
            int color = index == 0 ? ACCENT_COLOR : TEXT_COLOR;
            context.drawTextWithShadow(client.textRenderer, Text.literal(lines[index]).formatted(Formatting.RESET), x + 8, y + 6 + index * LINE_HEIGHT, color);
        }
    }

    private static int getPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) {
            return 0;
        }
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private static String formatCoordinate(double coordinate) {
        return String.format(Locale.ROOT, "%.1f", coordinate);
    }
}
