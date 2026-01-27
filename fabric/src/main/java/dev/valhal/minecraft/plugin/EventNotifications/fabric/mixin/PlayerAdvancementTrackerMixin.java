package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Shadow
    private Map<AdvancementEntry, AdvancementProgress> progress;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void onGrantCriterion(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
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
            if (!display.shouldAnnounceToChat()) return;

            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            String title = display.getTitle().getString();
            String description = display.getDescription().getString();
            String chatMessage = display.getFrame().getChatAnnouncementText(advancement, owner).getString();

            adapter.onPlayerAdvancement(
                    owner.getUuid(),
                    owner.getName().getString(),
                    title,
                    description,
                    chatMessage
            );
        } catch (Exception e) {
            FabricMod.log("PlayerAdvancementTrackerMixin error: {}", e.getMessage());
        }
    }
}
