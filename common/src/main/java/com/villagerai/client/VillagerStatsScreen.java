package com.villagerai.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class VillagerStatsScreen extends Screen {
    private final VillagerStatsData data;
    private final List<Component> lines = new ArrayList<>();

    public VillagerStatsScreen(VillagerStatsData data) {
        super(Component.literal("Villager Stats"));
        this.data = data;
        buildLines();
    }

    private void buildLines() {
        lines.clear();
        if (data.hasVillager) {
            lines.add(Component.literal("§6§l✦ Villager Stats ✦"));
            lines.add(Component.literal(String.format("§e%s §7(Level %d • %d XP) §8[%s§8]",
                data.professionName, data.level, data.xp, data.isBaby ? "§dBaby" : "§aAdult")));
            lines.add(Component.literal("---"));
            lines.add(Component.literal("§7• AI Mode: " + (data.noRoam ? "§aNo-Roam Active §8(Custom AI)" : "§eVanilla Roaming Active")));
            lines.add(Component.literal(String.format("§7• Current Time: §e%s §8(Tick %d, Day %d)", data.clockTime, data.dayTime, data.dayCount)));
            lines.add(Component.literal("§7• Scheduled Phase: §a" + data.scheduledPhase));
            lines.add(Component.literal("§7• Brain Activity: §b" + data.currentActivity));
            lines.add(Component.literal("§7• Workstation: §f" + data.workstationPos));
            lines.add(Component.literal("§7• Bed (Home): §f" + data.bedPos));
            lines.add(Component.literal("§7• Meeting Bell: §f" + data.bellPos));
            lines.add(Component.literal("§7• Walk Target: §f" + data.walkTargetInfo));
            lines.add(Component.literal("---"));
            lines.add(Component.literal("§8[Press V or ESC to Close]"));
        } else {
            lines.add(Component.literal("§6§l✦ Village Daily Schedule & Calendar ✦"));
            lines.add(Component.literal(String.format("§7Time: §e%s §7(Tick %d, Day %d) §8[Active: §a%s§8]",
                data.clockTime, data.dayTime, data.dayCount, data.scheduledPhase)));
            lines.add(Component.literal("---"));

            boolean isIdleMorning = data.dayTime < 2000L;
            boolean isWork = data.dayTime >= 2000L && data.dayTime < 9000L;
            boolean isMeet = data.dayTime >= 9000L && data.dayTime < 11000L;
            boolean isIdleEvening = data.dayTime >= 11000L && data.dayTime < 12000L;
            boolean isRest = data.dayTime >= 12000L;

            lines.add(Component.literal((isIdleMorning ? "§a§l▶ " : "§7• ") + "§e06:00 - 08:00 §8(10 - 2000t):   §fIDLE §8(Wake up & leisure)" + (isIdleMorning ? " §a◀ §8[NOW]" : "")));
            lines.add(Component.literal((isWork ? "§a§l▶ " : "§7• ") + "§a08:00 - 15:00 §8(2000 - 9000t):  §fWORK §8(Working at workstation)" + (isWork ? " §a◀ §8[NOW]" : "")));
            lines.add(Component.literal((isMeet ? "§a§l▶ " : "§7• ") + "§d15:00 - 17:00 §8(9000 - 11000t): §fMEET §8(Gathering at bell)" + (isMeet ? " §a◀ §8[NOW]" : "")));
            lines.add(Component.literal((isIdleEvening ? "§a§l▶ " : "§7• ") + "§e17:00 - 18:00 §8(11000 - 12000t): §fIDLE §8(Evening leisure)" + (isIdleEvening ? " §a◀ §8[NOW]" : "")));
            lines.add(Component.literal((isRest ? "§a§l▶ " : "§7• ") + "§918:00 - 06:00 §8(12000 - 24000t): §fREST §8(Sleeping in bed)" + (isRest ? " §a◀ §8[NOW]" : "")));
            lines.add(Component.literal("---"));
            lines.add(Component.literal("§7• Roaming: " + (data.noRoam ? "§aDisabled" : "§eEnabled") +
                " §7| Nearest Job: " + (data.assignNearest ? "§aEnabled" : "§eDisabled")));
            lines.add(Component.literal("§d💡 Aim at a villager to inspect their individual stats!"));
            lines.add(Component.literal("§8[Press V or ESC to Close]"));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Draw standard subtle background dimming
        this.extractTransparentBackground(extractor);

        // Calculate card dimensions dynamically based on content width
        int maxWidth = 0;
        for (Component line : lines) {
            if (!line.getString().equals("---")) {
                maxWidth = Math.max(maxWidth, this.font.width(line));
            }
        }
        int cardWidth = Math.max(maxWidth + 24, 260);
        int cardHeight = (lines.size() * 12) + 16;
        int startX = (this.width - cardWidth) / 2;
        int startY = (this.height - cardHeight) / 2;

        // Outer glow/shadow
        extractor.fill(startX - 2, startY - 2, startX + cardWidth + 2, startY + cardHeight + 2, 0x60000000);
        // Tooltip outer border
        extractor.fill(startX - 1, startY - 1, startX + cardWidth + 1, startY + cardHeight + 1, 0xFF35354A);
        // Tooltip deep translucent background
        extractor.fill(startX, startY, startX + cardWidth, startY + cardHeight, 0xF0101018);
        // Golden accent line on top edge
        extractor.fill(startX + 1, startY + 1, startX + cardWidth - 1, startY + 2, 0x80FFAA00);

        // Render each line
        int textY = startY + 10;
        for (Component line : lines) {
            if (line.getString().equals("---")) {
                // Horizontal divider line
                extractor.fill(startX + 10, textY + 3, startX + cardWidth - 10, textY + 4, 0x2AFFFFFF);
                textY += 8;
            } else {
                extractor.text(this.font, line, startX + 12, textY, 0xFFFFFFFF, true);
                textY += 12;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_V || event.key() == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
