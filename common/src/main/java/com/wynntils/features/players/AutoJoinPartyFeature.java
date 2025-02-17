/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.players;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.players.event.PartyEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.PLAYERS)
public class AutoJoinPartyFeature extends Feature {
    @RegisterConfig
    public final Config<Boolean> onlyFriends = new Config<>(true);

    @SubscribeEvent
    public void onPartyInvite(PartyEvent.Invited event) {
        if (onlyFriends.get() && !Models.Friends.isFriend(event.getPlayerName())) return;
        if (Models.Party.isInParty()) return;

        Managers.Notification.queueMessage(StyledText.fromString("Auto-joined " + event.getPlayerName() + "'s party"));
        McUtils.playSoundAmbient(SoundEvents.END_PORTAL_FRAME_FILL);

        Models.Party.partyJoin(event.getPlayerName());
    }
}
