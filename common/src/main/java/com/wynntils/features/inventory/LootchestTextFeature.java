/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class LootchestTextFeature extends Feature {
    @RegisterConfig
    private final Config<String> titleTextTemplate = new Config<>("§8Total: {chest_opened}");

    @RegisterConfig
    private final Config<String> inventoryTextTemplate = new Config<>("§8Dry: {dry_streak}");

    @SubscribeEvent
    public void onRenderLootChest(ContainerRenderEvent event) {
        if (!Models.Container.isLootChest(event.getScreen())) return;

        int startX = event.getScreen().leftPos;
        int startY = event.getScreen().topPos;
        int width = event.getScreen().imageWidth;
        int titleLabelX = event.getScreen().titleLabelX;
        int titleLabelY = event.getScreen().titleLabelY;
        int inventoryLabelX = event.getScreen().inventoryLabelX;
        int inventoryLabelY = event.getScreen().inventoryLabelY;

        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(startX, startY, 200);

        renderTotalChestCount(
                event.getPoseStack(), width - titleLabelX, titleLabelY, Models.LootChest.getOpenedChestCount());
        renderDryChestCount(
                event.getPoseStack(), width - inventoryLabelX, inventoryLabelY, Models.LootChest.getDryCount());

        poseStack.popPose();
    }

    private void renderTotalChestCount(PoseStack poseStack, int x, int y, int totalChests) {
        String titleTemplateResult = Arrays.stream(Managers.Function.doFormatLines(titleTextTemplate.get()))
                .map(StyledText::getString)
                .collect(Collectors.joining(" "));

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(titleTemplateResult),
                        x,
                        y,
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.RIGHT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE);
    }

    private void renderDryChestCount(PoseStack poseStack, int x, int y, int dryChests) {
        String inventoryTemplateResult = Arrays.stream(Managers.Function.doFormatLines(inventoryTextTemplate.get()))
                .map(StyledText::getString)
                .collect(Collectors.joining(" "));

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(inventoryTemplateResult),
                        x,
                        y,
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.RIGHT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE);
    }
}
