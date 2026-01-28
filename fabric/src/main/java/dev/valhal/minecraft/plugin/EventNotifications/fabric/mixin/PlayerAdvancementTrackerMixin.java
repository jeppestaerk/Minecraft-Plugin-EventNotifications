package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayer player;

    @Shadow
    private Map<AdvancementHolder, AdvancementProgress> progress;

    @Inject(method = "award", at = @At("RETURN"))
    private void onGrantCriterion(AdvancementHolder advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        try {
            // Only proceed if the criterion was actually granted
            if (!cir.getReturnValue()) return;

            // Check if the advancement is now complete
            AdvancementProgress advProgress = this.progress.get(advancement);
            if (advProgress == null || !advProgress.isDone()) return;

            // Skip recipe and hidden advancements
            if (advancement.value().display().isEmpty()) return;
            var display = advancement.value().display().get();
            if (display.isHidden()) return;

            // Only announce if configured to show in chat
            if (!display.shouldAnnounceChat()) return;

            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            String title = display.getTitle().getString();
            String description = display.getDescription().getString();
            String chatMessage = display.getType().createAnnouncement(advancement, player).getString();

            adapter.onPlayerAdvancement(
                    player.getUUID(),
                    player.getName().getString(),
                    title,
                    description,
                    chatMessage
            );
        } catch (Exception e) {
            FabricMod.log("PlayerAdvancementTrackerMixin error: {}", e.getMessage());
        }
    }
}
