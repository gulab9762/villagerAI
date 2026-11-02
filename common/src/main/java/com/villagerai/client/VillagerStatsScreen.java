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
        super(data.hasVillager
            ? Component.translatable("evv.gui.title.villager")
            : Component.translatable("evv.gui.title.calendar"));
        this.data = data;
        buildLines();
    }

    private void buildLines() {
        lines.clear();
        if (data.hasVillager) {
            lines.add(Component.translatable("evv.gui.header.villager"));
            lines.add(Component.translatable("evv.gui.profession_details",
                data.professionComponent,
                data.level,
                data.xp,
                data.isBaby ? Component.translatable("evv.gui.baby") : Component.translatable("evv.gui.adult")));
            lines.add(Component.literal("---"));
            lines.add(Component.translatable("evv.gui.ai_mode",
                data.noRoam ? Component.translatable("evv.gui.mode.no_roam") : Component.translatable("evv.gui.mode.vanilla")));
            lines.add(Component.translatable("evv.gui.current_time", data.clockTime, data.dayTime, data.dayCount));
            lines.add(Component.translatable("evv.gui.scheduled_phase", data.scheduledPhaseComponent));
            lines.add(Component.translatable("evv.gui.brain_activity", data.currentActivityComponent));
            lines.add(Component.translatable("evv.gui.workstation", data.workstationPosComponent));
            lines.add(Component.translatable("evv.gui.bed", data.bedPosComponent));
            lines.add(Component.translatable("evv.gui.bell", data.bellPosComponent));
            lines.add(Component.translatable("evv.gui.walk_target", data.walkTargetComponent));
            lines.add(Component.literal("---"));
            lines.add(Component.translatable("evv.gui.close_hint"));
        } else {
            lines.add(Component.translatable("evv.gui.header.calendar"));
            lines.add(Component.translatable("evv.gui.calendar.time",
                data.clockTime, data.dayTime, data.dayCount, data.scheduledPhaseComponent));
            lines.add(Component.literal("---"));

            boolean isIdleMorning = data.dayTime < 2000L;
            boolean isWork = data.dayTime >= 2000L && data.dayTime < 9000L;
            boolean isMeet = data.dayTime >= 9000L && data.dayTime < 11000L;
            boolean isIdleEvening = data.dayTime >= 11000L && data.dayTime < 12000L;
            boolean isRest = data.dayTime >= 12000L;

            lines.add(Component.literal(isIdleMorning ? "§a§l▶ " : "§7• ")
                .append(Component.translatable("evv.gui.schedule.idle_morning"))
                .append(isIdleMorning ? Component.translatable("evv.gui.now") : Component.empty()));
            lines.add(Component.literal(isWork ? "§a§l▶ " : "§7• ")
                .append(Component.translatable("evv.gui.schedule.work"))
                .append(isWork ? Component.translatable("evv.gui.now") : Component.empty()));
            lines.add(Component.literal(isMeet ? "§a§l▶ " : "§7• ")
                .append(Component.translatable("evv.gui.schedule.meet"))
                .append(isMeet ? Component.translatable("evv.gui.now") : Component.empty()));
            lines.add(Component.literal(isIdleEvening ? "§a§l▶ " : "§7• ")
                .append(Component.translatable("evv.gui.schedule.idle_evening"))
                .append(isIdleEvening ? Component.translatable("evv.gui.now") : Component.empty()));
            lines.add(Component.literal(isRest ? "§a§l▶ " : "§7• ")
                .append(Component.translatable("evv.gui.schedule.rest"))
                .append(isRest ? Component.translatable("evv.gui.now") : Component.empty()));
            lines.add(Component.literal("---"));
            lines.add(Component.translatable("evv.gui.settings",
                data.noRoam ? Component.translatable("evv.gui.disabled") : Component.translatable("evv.gui.enabled"),
                data.assignNearest ? Component.translatable("evv.gui.enabled") : Component.translatable("evv.gui.disabled")));
            lines.add(Component.translatable("evv.gui.aim_hint"));
            lines.add(Component.translatable("evv.gui.close_hint"));
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
