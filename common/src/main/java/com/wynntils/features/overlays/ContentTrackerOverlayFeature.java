/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.overlays.Overlay;
import com.wynntils.core.consumers.features.overlays.OverlayPosition;
import com.wynntils.core.consumers.features.overlays.OverlaySize;
import com.wynntils.core.consumers.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.models.activities.ActivityTrackerScoreboardPart;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.OVERLAYS)
public class ContentTrackerOverlayFeature extends Feature {
    @RegisterConfig
    public final Config<Boolean> disableTrackerOnScoreboard = new Config<>(true);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (Managers.Overlay.isEnabled(trackerOverlay)
                && disableTrackerOnScoreboard.get()
                && event.getSegment().getScoreboardPart() instanceof ActivityTrackerScoreboardPart) {
            event.setCanceled(true);
        }
    }

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final Overlay trackerOverlay = new ContentTrackerOverlay();

    public static class ContentTrackerOverlay extends Overlay {
        @RegisterConfig
        public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

        private static final List<CustomColor> TEXT_COLORS =
                List.of(CommonColors.GREEN, CommonColors.ORANGE, CommonColors.WHITE);

        private final List<TextRenderTask> toRender = createRenderTaskList();
        private final List<TextRenderTask> toRenderPreview = createRenderTaskList();

        protected ContentTrackerOverlay() {
            super(
                    new OverlayPosition(
                            5,
                            -5,
                            VerticalAlignment.TOP,
                            HorizontalAlignment.RIGHT,
                            OverlayPosition.AnchorSection.TOP_RIGHT),
                    new OverlaySize(300, 50),
                    HorizontalAlignment.LEFT,
                    VerticalAlignment.MIDDLE);

            toRenderPreview
                    .get(0)
                    .setText(I18n.get("feature.wynntils.contentTrackerOverlay.overlay.contentTracker.title")
                            + " Quest:");
            toRenderPreview
                    .get(1)
                    .setText(I18n.get("feature.wynntils.contentTrackerOverlay.overlay.contentTracker.testQuestName")
                            + ":");
            toRenderPreview
                    .get(2)
                    .setText(
                            """
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer \
                            tempus purus in lacus pulvinar dictum. Quisque suscipit erat \
                            pellentesque egestas volutpat. \
                            """);
        }

        private List<TextRenderTask> createRenderTaskList() {
            List<TextRenderTask> renderTaskList = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                renderTaskList.add(new TextRenderTask(
                        StyledText.EMPTY,
                        TextRenderSetting.DEFAULT
                                .withMaxWidth(this.getWidth())
                                .withCustomColor(TEXT_COLORS.get(i))
                                .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                                .withTextShadow(this.textShadow.get())));
            }
            return renderTaskList;
        }

        private void updateTextRenderSettings(List<TextRenderTask> renderTasks) {
            for (int i = 0; i < 3; i++) {
                renderTasks
                        .get(i)
                        .setSetting(TextRenderSetting.DEFAULT
                                .withMaxWidth(this.getWidth())
                                .withCustomColor(TEXT_COLORS.get(i))
                                .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                                .withTextShadow(this.textShadow.get()));
            }
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {
            updateTextRenderSettings(toRender);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
            if (!Models.Activity.isTracking()) {
                return;
            }

            toRender.get(0)
                    .setText(I18n.get("feature.wynntils.contentTrackerOverlay.overlay.contentTracker.title") + " "
                            + Models.Activity.getTrackedType().getDisplayName() + ":");
            toRender.get(1).setText(Models.Activity.getTrackedName());
            toRender.get(2).setText(Models.Activity.getTrackedTask());

            BufferedFontRenderer.getInstance()
                    .renderTextsWithAlignment(
                            poseStack,
                            bufferSource,
                            this.getRenderX(),
                            this.getRenderY(),
                            toRender,
                            this.getWidth(),
                            this.getHeight(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());
        }

        @Override
        public void renderPreview(
                PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
            updateTextRenderSettings(toRenderPreview); // we have to force update every time

            BufferedFontRenderer.getInstance()
                    .renderTextsWithAlignment(
                            poseStack,
                            bufferSource,
                            this.getRenderX(),
                            this.getRenderY(),
                            toRenderPreview,
                            this.getWidth(),
                            this.getHeight(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());
        }
    }
}
