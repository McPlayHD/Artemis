/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.character.event.CharacterDeathEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.StyledTextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.CHAT)
public class DeathCoordinatesFeature extends Feature {
    @SubscribeEvent
    public void onCharacterDeath(CharacterDeathEvent e) {
        StyledText deathMessage =
                StyledText.fromComponent(Component.translatable("feature.wynntils.deathCoordinates.diedAt")
                        .withStyle(ChatFormatting.DARK_RED));
        deathMessage = deathMessage.appendPart(StyledTextUtils.createLocationPart(e.getLocation()));

        McUtils.player().sendSystemMessage(deathMessage.getComponent());
    }
}
