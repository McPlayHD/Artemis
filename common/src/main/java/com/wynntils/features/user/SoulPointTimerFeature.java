/*
 * Copyright © Wynntils 2021-2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.ItemsReceivedEvent;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.mc.utils.objects.DynamicTag;
import com.wynntils.wc.utils.WynnItemMatchers;
import com.wynntils.wc.utils.WynnUtils;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE)
public class SoulPointTimerFeature extends UserFeature {
    @SubscribeEvent
    public void onItemsReceived(ItemsReceivedEvent e) {
        if (!WynnUtils.onServer()) return;

        // The event can get called even with a container without the Soul Points item
        ItemStack soulPointStack = findSoulPointStack(e.getItems());
        if (soulPointStack == null) return;

        ListTag lore = ItemUtils.getLoreTag(soulPointStack);

        if (lore == null) {
            lore = new ListTag();
        } else {
            lore.add(StringTag.valueOf("")); // Equivalent to adding ""
        }

        lore.add(new DynamicTag(() -> {
            int rawSecondsUntilSoulPoint = getTicksTillNextSoulPoint() / 20;
            int minutesUntilSoulPoint = rawSecondsUntilSoulPoint / 60;
            int secondsUntilSoulPoint = rawSecondsUntilSoulPoint % 60;

            return ItemUtils.toLoreString(ChatFormatting.AQUA + "Time until next soul point: " + ChatFormatting.WHITE
                    + minutesUntilSoulPoint + ":" + String.format("%02d", secondsUntilSoulPoint));
        }));

        ItemUtils.replaceLore(soulPointStack, lore);
    }

    private static ItemStack findSoulPointStack(List<ItemStack> items) {
        return items.stream().filter(WynnItemMatchers::isSoulPoint).findFirst().orElse(null);
    }

    /**
     * @return Time in game ticks (1/20th of a second, 50ms) until next soul point
     *     <p>-1 if unable to determine
     *     <p>Also check that {@code {@link #getMaxSoulPoints()} >= {@link #getSoulPoints()}}, in
     *     which case soul points are already full
     */
    private static int getTicksTillNextSoulPoint() {
        if (McUtils.mc().level == null) return -1;

        return 24000 - (int) (McUtils.mc().level.getDayTime() % 24000);
    }
}
