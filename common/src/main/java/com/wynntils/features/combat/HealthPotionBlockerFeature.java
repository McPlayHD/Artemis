/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.mc.event.PlayerInteractEvent;
import com.wynntils.mc.event.UseItemEvent;
import com.wynntils.models.elements.type.PotionType;
import com.wynntils.models.items.items.game.CraftedConsumableItem;
import com.wynntils.models.items.items.game.MultiHealthPotionItem;
import com.wynntils.models.items.items.game.PotionItem;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class HealthPotionBlockerFeature extends Feature {
    @RegisterConfig
    public final Config<Integer> threshold = new Config<>(95);

    @SubscribeEvent
    public void onPotionUse(UseItemEvent event) {
        if (checkPotionUse()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPotionUseOn(PlayerInteractEvent event) {
        if (checkPotionUse()) {
            event.setCanceled(true);
        }
    }

    private boolean checkPotionUse() {
        ItemStack itemStack = McUtils.inventory().getSelected();
        if (!isHealingPotion(itemStack)) return false;

        CappedValue health = Models.CharacterStats.getHealth();
        int percentage = health.getPercentageInt();

        if (percentage >= threshold.get()) {
            MutableComponent response = (percentage < 100)
                    ? Component.translatable("feature.wynntils.healthPotionBlocker.thresholdReached", percentage)
                    : Component.translatable("feature.wynntils.healthPotionBlocker.healthFull");
            Managers.Notification.queueMessage(response.withStyle(ChatFormatting.RED));
            return true;
        }

        return false;
    }

    private boolean isHealingPotion(ItemStack itemStack) {
        Optional<MultiHealthPotionItem> healthPotionOpt =
                Models.Item.asWynnItem(itemStack, MultiHealthPotionItem.class);
        Optional<PotionItem> potionOpt = Models.Item.asWynnItem(itemStack, PotionItem.class);
        Optional<CraftedConsumableItem> craftedConsumableOpt =
                Models.Item.asWynnItem(itemStack, CraftedConsumableItem.class);

        if (healthPotionOpt.isEmpty() && potionOpt.isEmpty() && craftedConsumableOpt.isEmpty()) return false;

        // Check if potion is a healing potion
        if (potionOpt.isPresent()) {
            return (potionOpt.get().getType() == PotionType.HEALING);
        }

        // Check if crafted potion is a health potion
        if (craftedConsumableOpt.isPresent()) {
            return craftedConsumableOpt.get().isHealing();
        }

        // Multi health potions are always healing potions
        return true;
    }
}
