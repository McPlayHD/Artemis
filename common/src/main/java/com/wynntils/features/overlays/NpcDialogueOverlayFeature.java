/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
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
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.NpcDialogEvent;
import com.wynntils.handlers.chat.type.NpcDialogueType;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.OVERLAYS)
public class NpcDialogueOverlayFeature extends Feature {
    // §6§lNew Quest Started: §e§lEnzan's Brother
    private static final Pattern NEW_QUEST_STARTED = Pattern.compile("^§6§lNew Quest Started: §e§l(.*)$");
    private static final StyledText PRESS_SNEAK_TO_CONTINUE = StyledText.fromString("§cPress SNEAK to continue");

    private final ScheduledExecutorService autoProgressExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledAutoProgressKeyPress = null;

    private final List<ConfirmationlessDialogue> confirmationlessDialogues = new ArrayList<>();
    private List<StyledText> currentDialogue = new ArrayList<>();
    private NpcDialogueType dialogueType;
    private boolean isProtected;

    @RegisterConfig
    public final Config<Boolean> autoProgress = new Config<>(false);

    @RegisterConfig
    public final Config<Integer> dialogAutoProgressDefaultTime = new Config<>(1600); // Milliseconds

    @RegisterConfig
    public final Config<Integer> dialogAutoProgressAdditionalTimePerWord = new Config<>(300); // Milliseconds

    @RegisterKeyBind
    public final KeyBind cancelAutoProgressKeybind =
            new KeyBind("Cancel Dialog Auto Progress", GLFW.GLFW_KEY_Y, false, this::cancelAutoProgress);

    private void cancelAutoProgress() {
        if (scheduledAutoProgressKeyPress == null) return;

        scheduledAutoProgressKeyPress.cancel(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onNpcDialogue(NpcDialogEvent e) {
        List<StyledText> msg =
                e.getChatMessage().stream().map(StyledText::fromComponent).toList();

        // Print dialogue to the system log
        WynntilsMod.info("[NPC] Type: " + (msg.isEmpty() ? "<empty> " : "") + (e.isProtected() ? "<protected> " : "")
                + e.getType());
        msg.forEach(s -> WynntilsMod.info("[NPC] " + (s.isEmpty() ? "<empty>" : s)));

        // The same message can be repeating before we have finished removing the old
        // Just remove the old and add the new with an updated remove time
        // It can also happen that a confirmationless dialogue turn into a normal
        // dialogue after a while (the "Press SHIFT..." text do not appear immediately)
        confirmationlessDialogues.removeIf(d -> d.text.equals(msg));

        if (e.getType() == NpcDialogueType.CONFIRMATIONLESS) {
            ConfirmationlessDialogue dialogue =
                    new ConfirmationlessDialogue(msg, System.currentTimeMillis() + calculateMessageReadTime(msg));
            confirmationlessDialogues.add(dialogue);
            return;
        }

        currentDialogue = msg;
        dialogueType = e.getType();
        isProtected = e.isProtected();

        if (!msg.isEmpty() && msg.get(0).getMatcher(NEW_QUEST_STARTED).find()) {
            // TODO: Show nice banner notification instead
            // but then we'd also need to confirm it with a sneak
            Managers.Notification.queueMessage(msg.get(0));
        }

        if (e.getType() == NpcDialogueType.SELECTION && !e.isProtected()) {
            // This is a bit of a workaround to be able to select the options
            MutableComponent clickMsg =
                    Component.literal("Select an option to continue:").withStyle(ChatFormatting.AQUA);
            e.getChatMessage()
                    .forEach(line -> clickMsg.append(Component.literal("\n").append(line)));
            McUtils.sendMessageToClient(clickMsg);
        }

        if (scheduledAutoProgressKeyPress != null) {
            scheduledAutoProgressKeyPress.cancel(true);

            // Release sneak key if currently pressed
            McUtils.sendPacket(new ServerboundPlayerCommandPacket(
                    McUtils.player(), ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));

            scheduledAutoProgressKeyPress = null;
        }

        if (autoProgress.get() && dialogueType == NpcDialogueType.NORMAL) {
            // Schedule a new sneak key press if this is not the end of the dialogue
            if (!msg.isEmpty()) {
                scheduledAutoProgressKeyPress = scheduledSneakPress(msg);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        long now = System.currentTimeMillis();
        confirmationlessDialogues.removeIf(dialogue -> now >= dialogue.removeTime);
    }

    private ScheduledFuture<?> scheduledSneakPress(List<StyledText> msg) {
        long delay = calculateMessageReadTime(msg);

        return autoProgressExecutor.schedule(
                () -> McUtils.sendPacket(new ServerboundPlayerCommandPacket(
                        McUtils.player(), ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY)),
                delay,
                TimeUnit.MILLISECONDS);
    }

    private long calculateMessageReadTime(List<StyledText> msg) {
        int words = StyledText.join(" ", msg).split(" ").length;
        long delay =
                dialogAutoProgressDefaultTime.get() + ((long) words * dialogAutoProgressAdditionalTimePerWord.get());
        return delay;
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent e) {
        currentDialogue = List.of();
        confirmationlessDialogues.clear();
        cancelAutoProgress();
    }

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final Overlay npcDialogueOverlay = new NpcDialogueOverlay();

    public class NpcDialogueOverlay extends Overlay {
        @RegisterConfig
        public final Config<TextShadow> textShadow = new Config<>(TextShadow.NORMAL);

        @RegisterConfig
        public final Config<Float> backgroundOpacity = new Config<>(0.2f);

        @RegisterConfig
        public final Config<Boolean> stripColors = new Config<>(false);

        @RegisterConfig
        public final Config<Boolean> showHelperTexts = new Config<>(true);

        private TextRenderSetting renderSetting;

        protected NpcDialogueOverlay() {
            super(
                    new OverlayPosition(
                            0,
                            0,
                            VerticalAlignment.TOP,
                            HorizontalAlignment.CENTER,
                            OverlayPosition.AnchorSection.BOTTOM_MIDDLE),
                    new OverlaySize(400, 50),
                    HorizontalAlignment.CENTER,
                    VerticalAlignment.MIDDLE);
            updateTextRenderSettings();
        }

        private void updateTextRenderSettings() {
            renderSetting = TextRenderSetting.DEFAULT
                    .withMaxWidth(this.getWidth() - 5)
                    .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                    .withTextShadow(textShadow.get());
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {
            updateDialogExtractionSettings();
            updateTextRenderSettings();
        }

        private void updateDialogExtractionSettings() {
            if (Managers.Overlay.isEnabled(this)) {
                Handlers.Chat.addNpcDialogExtractionDependent(NpcDialogueOverlayFeature.this);
            } else {
                Handlers.Chat.removeNpcDialogExtractionDependent(NpcDialogueOverlayFeature.this);
                currentDialogue = List.of();
                confirmationlessDialogues.clear();
            }
        }

        private void renderDialogue(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                List<StyledText> currentDialogue,
                NpcDialogueType dialogueType) {
            List<TextRenderTask> dialogueRenderTasks = currentDialogue.stream()
                    .map(s -> new TextRenderTask(s, renderSetting))
                    .toList();

            if (stripColors.get()) {
                dialogueRenderTasks.forEach(dialogueRenderTask ->
                        dialogueRenderTask.setText(dialogueRenderTask.getText().getStringWithoutFormatting()));
            }

            float textHeight = (float) dialogueRenderTasks.stream()
                    .map(dialogueRenderTask -> FontRenderer.getInstance()
                            .calculateRenderHeight(
                                    dialogueRenderTask.getText(),
                                    dialogueRenderTask.getSetting().maxWidth()))
                    .mapToDouble(f -> f)
                    .sum();

            // Draw a translucent background
            float rectHeight = textHeight + 10;
            float rectRenderY =
                    switch (this.getRenderVerticalAlignment()) {
                        case TOP -> this.getRenderY();
                        case MIDDLE -> this.getRenderY() + (this.getHeight() - rectHeight) / 2f;
                        case BOTTOM -> this.getRenderY() + this.getHeight() - rectHeight;
                    };
            int colorAlphaRect = Math.round(MathUtils.clamp(255 * backgroundOpacity.get(), 0, 255));
            BufferedRenderUtils.drawRect(
                    poseStack,
                    bufferSource,
                    CommonColors.BLACK.withAlpha(colorAlphaRect),
                    this.getRenderX(),
                    rectRenderY,
                    0,
                    this.getWidth(),
                    rectHeight);

            // Render the message
            BufferedFontRenderer.getInstance()
                    .renderTextsWithAlignment(
                            poseStack,
                            bufferSource,
                            this.getRenderX(),
                            this.getRenderY(),
                            dialogueRenderTasks,
                            this.getWidth(),
                            this.getHeight(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());

            if (showHelperTexts.get()) {
                // Render "To continue" message
                List<TextRenderTask> renderTaskList = new LinkedList<>();
                StyledText protection = isProtected ? StyledText.fromString("§f<protected> §r") : StyledText.EMPTY;
                if (dialogueType == NpcDialogueType.NORMAL) {
                    TextRenderTask pressSneakMessage =
                            new TextRenderTask(PRESS_SNEAK_TO_CONTINUE.prepend(protection), renderSetting);
                    renderTaskList.add(pressSneakMessage);
                } else if (dialogueType == NpcDialogueType.SELECTION) {
                    String msg;
                    if (isProtected) {
                        msg = "Select an option to continue (Press the number key to select it)";
                    } else {
                        msg = "Open chat and click on the option to select it";
                    }

                    TextRenderTask pressSneakMessage = new TextRenderTask(protection.append("§c" + msg), renderSetting);
                    renderTaskList.add(pressSneakMessage);
                }

                if (scheduledAutoProgressKeyPress != null && !scheduledAutoProgressKeyPress.isCancelled()) {
                    long timeUntilProgress = scheduledAutoProgressKeyPress.getDelay(TimeUnit.MILLISECONDS);
                    TextRenderTask autoProgressMessage = new TextRenderTask(
                            ChatFormatting.GREEN + "Auto-progress: "
                                    + Math.max(0, Math.round(timeUntilProgress / 1000f))
                                    + " seconds (Press "
                                    + StyledText.fromComponent(cancelAutoProgressKeybind
                                                    .getKeyMapping()
                                                    .getTranslatedKeyMessage())
                                            .getStringWithoutFormatting()
                                    + " to cancel)",
                            renderSetting);
                    renderTaskList.add(autoProgressMessage);
                }

                BufferedFontRenderer.getInstance()
                        .renderTextsWithAlignment(
                                poseStack,
                                bufferSource,
                                this.getRenderX() + 5,
                                this.getRenderY() + 20 + textHeight,
                                renderTaskList,
                                this.getWidth() - 30,
                                this.getHeight() - 30,
                                this.getRenderHorizontalAlignment(),
                                this.getRenderVerticalAlignment());
            }
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
            if (currentDialogue.isEmpty() && confirmationlessDialogues.isEmpty()) return;

            LinkedList<StyledText> allDialogues = new LinkedList<>(currentDialogue);
            confirmationlessDialogues.forEach(d -> {
                allDialogues.add(StyledText.EMPTY);
                allDialogues.addAll(d.text());
            });

            if (currentDialogue.isEmpty()) {
                // Remove the initial blank line in that case
                allDialogues.removeFirst();
            }
            renderDialogue(poseStack, bufferSource, allDialogues, dialogueType);
        }

        @Override
        public void renderPreview(
                PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
            List<StyledText> fakeDialogue = List.of(
                    StyledText.fromString(
                            "§7[1/1] §r§2Random Citizen: §r§aDid you know that Wynntils is the best Wynncraft mod you'll probably find?§r"));
            // we have to force update every time
            updateTextRenderSettings();

            renderDialogue(poseStack, bufferSource, fakeDialogue, NpcDialogueType.NORMAL);
        }
    }

    protected record ConfirmationlessDialogue(List<StyledText> text, long removeTime) {}
}
