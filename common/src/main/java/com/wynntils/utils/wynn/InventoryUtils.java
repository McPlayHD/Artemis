/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.wynn;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.properties.GearTypeItemProperty;
import com.wynntils.utils.mc.McUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public final class InventoryUtils {
    public static final int COMPASS_SLOT_NUM = 6;
    public static final int CONTENT_BOOK_SLOT_NUM = 7;
    public static final int SOUL_POINTS_SLOT_NUM = 8;
    public static final int INGREDIENT_POUCH_SLOT_NUM = 13;

    private static final int RING_1_SLOT_NUM = 9;
    private static final int RING_2_SLOT_NUM = 10;
    private static final int BRACELET_SLOT_NUM = 11;
    private static final int NECKLACE_SLOT_NUM = 12;
    private static final List<Integer> ACCESSORY_SLOTS =
            List.of(RING_1_SLOT_NUM, RING_2_SLOT_NUM, BRACELET_SLOT_NUM, NECKLACE_SLOT_NUM);

    public static void sendInventorySlotMouseClick(int slotNumber, MouseClickType mouseButton) {
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
        ItemStack itemStack = McUtils.inventory().getItem(slotNumber);
        changedSlots.put(slotNumber, itemStack);

        McUtils.sendPacket(new ServerboundContainerClickPacket(
                McUtils.inventoryMenu().containerId,
                McUtils.inventoryMenu().getStateId(),
                slotNumber,
                mouseButton.ordinal(),
                ClickType.PICKUP,
                ItemStack.EMPTY,
                changedSlots));
    }

    public static boolean isWeapon(ItemStack itemStack) {
        Optional<GearTypeItemProperty> gearItemOpt =
                Models.Item.asWynnItemPropery(itemStack, GearTypeItemProperty.class);
        if (gearItemOpt.isEmpty()) return false;

        return gearItemOpt.get().getGearType().isWeapon();
    }

    public static List<ItemStack> getAccessories(Player player) {
        List<ItemStack> accessories = new ArrayList<>();
        ACCESSORY_SLOTS.forEach(
                slot -> accessories.add(player.getInventory().items.get(slot)));
        return accessories;
    }

    public static boolean isItemListsEqual(List<ItemStack> firstItems, List<ItemStack> secondItems) {
        if (firstItems.size() != secondItems.size()) return false;

        for (int i = 0; i < firstItems.size(); i++) {
            ItemStack newItem = firstItems.get(i);
            ItemStack oldItem = secondItems.get(i);
            if (!newItem.getItem().equals(oldItem.getItem())
                    || newItem.getDamageValue() != oldItem.getDamageValue()
                    || newItem.getCount() != oldItem.getCount()
                    || !ItemStack.tagMatches(oldItem, newItem)) {
                return false;
            }
        }
        return true;
    }

    public static StyledText getItemName(ItemStack itemStack) {
        return StyledText.fromComponent(itemStack.getHoverName());
    }

    public enum MouseClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }
}
