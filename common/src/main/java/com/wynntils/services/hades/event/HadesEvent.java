/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades.event;

import com.wynntils.core.events.EventThread;
import net.minecraftforge.eventbus.api.Event;

public abstract class HadesEvent extends Event {
    @EventThread(EventThread.Type.IO)
    public static class Authenticated extends HadesEvent {}

    public static class Disconnected extends HadesEvent {}
}
