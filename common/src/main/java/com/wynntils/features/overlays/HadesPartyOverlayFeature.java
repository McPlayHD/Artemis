/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.overlays.ContainerOverlay;
import com.wynntils.core.consumers.features.overlays.Overlay;
import com.wynntils.core.consumers.features.overlays.OverlayPosition;
import com.wynntils.core.consumers.features.overlays.OverlaySize;
import com.wynntils.core.consumers.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.models.players.event.HadesRelationsUpdateEvent;
import com.wynntils.models.players.event.PartyEvent;
import com.wynntils.models.players.scoreboard.PartyScoreboardPart;
import com.wynntils.services.hades.HadesUser;
import com.wynntils.services.hades.event.HadesUserAddedEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.SkinUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HealthTexture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.ManaTexture;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.OVERLAYS)
public class HadesPartyOverlayFeature extends Feature {
    @RegisterConfig
    public final Config<Boolean> disablePartyMembersOnScoreboard = new Config<>(false);

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final PartyMembersOverlay partyMembersOverlay = new PartyMembersOverlay(
            new OverlayPosition(
                    70, 5, VerticalAlignment.TOP, HorizontalAlignment.LEFT, OverlayPosition.AnchorSection.MIDDLE_LEFT),
            new OverlaySize(162, 50),
            ContainerOverlay.GrowDirection.DOWN,
            HorizontalAlignment.LEFT,
            VerticalAlignment.TOP);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (Managers.Overlay.isEnabled(partyMembersOverlay)
                && disablePartyMembersOnScoreboard.get()
                && event.getSegment().getScoreboardPart() instanceof PartyScoreboardPart) {
            event.setCanceled(true);
        }
    }

    public class PartyMemberOverlay extends Overlay {
        private static final int HEAD_SIZE = 26;

        private final HadesUser hadesUser;

        protected PartyMemberOverlay(HadesUser hadesUser) {
            super(
                    new OverlayPosition(
                            0,
                            0,
                            VerticalAlignment.TOP,
                            HorizontalAlignment.LEFT,
                            OverlayPosition.AnchorSection.TOP_LEFT),
                    new OverlaySize(162, 50));
            this.hadesUser = hadesUser;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
            poseStack.pushPose();

            ResourceLocation skin = SkinUtils.getSkin(hadesUser.getUuid());

            float renderX = getRenderX();
            float renderY = getRenderY();

            poseStack.translate(renderX, renderY, 0);

            // head
            BufferedRenderUtils.drawTexturedRect(
                    poseStack, bufferSource, skin, 0, 0, 0, HEAD_SIZE, HEAD_SIZE, 8, 8, 8, 8, 64, 64);

            // hat
            BufferedRenderUtils.drawTexturedRect(
                    poseStack, bufferSource, skin, 0, 0, 1, HEAD_SIZE, HEAD_SIZE, 40, 8, 8, 8, 64, 64);

            poseStack.translate(HEAD_SIZE, 0, 0);

            poseStack.translate(3, 0, 0);

            BufferedFontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            bufferSource,
                            StyledText.fromString(hadesUser.getName()),
                            0,
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.NORMAL);

            poseStack.translate(0, 12, 0);

            double healthProgress = hadesUser.getHealth().getProgress();
            double manaProgress = hadesUser.getMana().getProgress();

            // health
            HealthTexture healthTexture = HadesPartyOverlayFeature.this.partyMembersOverlay.healthTexture.get();
            BufferedRenderUtils.drawProgressBar(
                    poseStack,
                    bufferSource,
                    Texture.HEALTH_BAR,
                    0,
                    0,
                    81 * 0.85f,
                    healthTexture.getHeight() * 0.85f,
                    0,
                    healthTexture.getTextureY1(),
                    81,
                    healthTexture.getTextureY2(),
                    (float) healthProgress);

            poseStack.translate(0, healthTexture.getHeight() * 0.85f, 0);

            // mana
            ManaTexture manaTexture = HadesPartyOverlayFeature.this.partyMembersOverlay.manaTexture.get();
            BufferedRenderUtils.drawProgressBar(
                    poseStack,
                    bufferSource,
                    Texture.MANA_BAR,
                    0,
                    2,
                    81 * 0.85f,
                    2 + manaTexture.getHeight() * 0.85f,
                    0,
                    manaTexture.getTextureY1(),
                    81,
                    manaTexture.getTextureY2(),
                    (float) manaProgress);

            poseStack.popPose();
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {}
    }

    protected class PartyMembersOverlay extends ContainerOverlay<PartyMemberOverlay> {
        private static final HadesUser DUMMY_USER_1 =
                new HadesUser("Player 1", new CappedValue(12432, 13120), new CappedValue(65, 123));
        private static final HadesUser DUMMY_USER_2 =
                new HadesUser("Player 2", new CappedValue(4561, 9870), new CappedValue(98, 170));

        @RegisterConfig
        public final Config<Integer> maxPartyMembers = new Config<>(4);

        @RegisterConfig
        public final Config<HealthTexture> healthTexture = new Config<>(HealthTexture.A);

        @RegisterConfig
        public final Config<ManaTexture> manaTexture = new Config<>(ManaTexture.A);

        protected PartyMembersOverlay(
                OverlayPosition position,
                OverlaySize size,
                GrowDirection growDirection,
                HorizontalAlignment horizontalAlignment,
                VerticalAlignment verticalAlignment) {
            super(position, size, growDirection, horizontalAlignment, verticalAlignment);
        }

        @Override
        protected List<PartyMemberOverlay> getPreviewChildren() {
            return List.of(new PartyMemberOverlay(DUMMY_USER_1), new PartyMemberOverlay(DUMMY_USER_2));
        }

        @SubscribeEvent
        public void onHadesUserAdded(HadesUserAddedEvent event) {
            updateChildren();
        }

        @SubscribeEvent
        public void onPartyChange(HadesRelationsUpdateEvent.PartyList event) {
            updateChildren();
        }

        @SubscribeEvent
        public void onPartyChange(PartyEvent event) {
            updateChildren();
        }

        private void updateChildren() {
            this.clearChildren();

            // War users take priority
            List<HadesUser> warUsers = Models.War.getHadesUsers();

            if (!warUsers.isEmpty()) {
                warUsers.forEach(hadesUser -> this.addChild(new PartyMemberOverlay(hadesUser)));
                return;
            }

            List<HadesUser> hadesUsers = Services.Hades.getHadesUsers().toList();
            List<String> partyMembers = Models.Party.getPartyMembers();

            List<HadesUser> hadesUsingPartyMembers = hadesUsers.stream()
                    .filter(hadesUser -> partyMembers.contains(hadesUser.getName()))
                    .sorted(Comparator.comparing(element -> partyMembers.indexOf(element.getName())))
                    .collect(Collectors.toList());
            hadesUsingPartyMembers =
                    hadesUsingPartyMembers.subList(0, Math.min(hadesUsingPartyMembers.size(), maxPartyMembers.get()));

            for (HadesUser hadesUser : hadesUsingPartyMembers) {
                this.addChild(new PartyMemberOverlay(hadesUser));
            }
        }
    }
}
