/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CosmeticsPreviewFeature extends Feature {
    private static final String WEAPON_COSMETICS_TITLE = "Weapon Cosmetics";
    private static final String HELMET_COSMETICS_TITLE = "Helmet Cosmetics";
    private static final String GUILD_GEAR_MENU_TITLE = "Guild Cosmetics";

    @SubscribeEvent
    public void onContainerRender(ContainerRenderEvent event) {
        AbstractContainerScreen<?> screen = event.getScreen();
        String title = StyledText.fromComponent(screen.getTitle()).getStringWithoutFormatting();

        if (title.equals(WEAPON_COSMETICS_TITLE)
                || title.equals(HELMET_COSMETICS_TITLE)
                || title.equals(GUILD_GEAR_MENU_TITLE)) {
            int posX = screen.leftPos + screen.imageWidth + 20;
            int posY = screen.topPos + screen.imageHeight / 2;

            PoseStack poseStack = new PoseStack();
            RenderUtils.drawTexturedRect(poseStack, Texture.COSMETIC_VIEWER_BACKGROUND, posX - 15, posY - 75);

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    poseStack,
                    posX + 20,
                    posY,
                    30,
                    posX + 20 - event.getMouseX(),
                    posY - 50 - event.getMouseY(),
                    McUtils.player());
        }
    }
}
