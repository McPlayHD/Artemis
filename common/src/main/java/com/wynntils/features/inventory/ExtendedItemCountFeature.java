/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.ItemCountOverlayRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.properties.CountedItemProperty;
import com.wynntils.models.items.properties.LeveledItemProperty;
import com.wynntils.models.items.properties.UsesItemPropery;
import com.wynntils.utils.mc.KeyboardUtils;
import java.util.Optional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class ExtendedItemCountFeature extends Feature {
    @RegisterConfig
    public final Config<Boolean> inventoryTextOverlayEnabled = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> hotbarTextOverlayEnabled = new Config<>(true);

    private boolean isInventory;

    @SubscribeEvent
    public void onRenderSlotPre(SlotRenderEvent.Pre e) {
        isInventory = true;
    }

    @SubscribeEvent
    public void onRenderHotbarSlotPre(HotbarSlotRenderEvent.Pre e) {
        isInventory = false;
    }

    @SubscribeEvent
    public void onItemCountOverlay(ItemCountOverlayRenderEvent event) {
        if (isInventory && !inventoryTextOverlayEnabled.get()) return;
        if (!isInventory && !hotbarTextOverlayEnabled.get()) return;

        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(event.getItemStack());
        if (wynnItemOpt.isEmpty()) return;

        WynnItem wynnItem = wynnItemOpt.get();

        if (wynnItem instanceof LeveledItemProperty leveledItem
                && KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)
                && isInventory) {
            event.setCountString(String.valueOf(leveledItem.getLevel()));
        } else if (wynnItem instanceof UsesItemPropery usesItem && usesItem.hasCount()) {
            event.setCountString(
                    String.valueOf(usesItem.getCount() * event.getItemStack().getCount()));
            event.setCountColor(usesItem.getCountColor().asInt());
        } else if (wynnItem instanceof CountedItemProperty countedItem && countedItem.hasCount()) {
            event.setCountString(String.valueOf(countedItem.getCount()));
            event.setCountColor(countedItem.getCountColor().asInt());
        }
    }
}
