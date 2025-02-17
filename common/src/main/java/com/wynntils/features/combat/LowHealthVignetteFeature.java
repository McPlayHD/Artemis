/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class LowHealthVignetteFeature extends Feature {
    private static final float INTENSITY = 0.3f;

    @RegisterConfig
    public final Config<Integer> lowHealthPercentage = new Config<>(25);

    @RegisterConfig
    public final Config<Float> animationSpeed = new Config<>(0.6f);

    @RegisterConfig
    public final Config<HealthVignetteEffect> healthVignetteEffect = new Config<>(HealthVignetteEffect.PULSE);

    @RegisterConfig
    public final Config<CustomColor> color = new Config<>(new CustomColor(255, 0, 0));

    private float animation = 10f;
    private float value = INTENSITY;
    private boolean shouldRender = false;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderGui(RenderEvent.Post event) {
        if (!shouldRender || event.getType() != RenderEvent.ElementType.GUI) return;
        if (!Models.WorldState.onWorld()) return;

        RenderUtils.renderVignetteOverlay(event.getPoseStack(), color.get(), value);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        float healthProgress = (float) Models.CharacterStats.getHealth().getProgress();
        float threshold = lowHealthPercentage.get() / 100f;
        shouldRender = false;

        if (healthProgress > threshold) return;
        shouldRender = true;

        switch (healthVignetteEffect.get()) {
            case PULSE -> {
                animation = (animation + animationSpeed.get()) % 40;
                value = threshold - healthProgress * INTENSITY + 0.01f * Math.abs(20 - animation);
            }
            case GROWING -> value = MathUtils.map(healthProgress, 0, threshold, INTENSITY, 0.1f);
            case STATIC -> value = INTENSITY;
        }
    }

    public enum HealthVignetteEffect {
        PULSE,
        GROWING,
        STATIC
    }
}
