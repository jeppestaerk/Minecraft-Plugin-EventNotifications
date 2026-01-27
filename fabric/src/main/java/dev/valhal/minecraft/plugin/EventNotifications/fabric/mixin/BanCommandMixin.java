package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(BanCommand.class)
public class BanCommandMixin {
    @Inject(method = "ban", at = @At("HEAD"))
    private static void onBan(ServerCommandSource source, Collection<PlayerConfigEntry> targets, @Nullable Text reason, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            String bannedBy = source.getName();
            String reasonStr = reason != null ? reason.getString() : "No reason given";

            for (PlayerConfigEntry entry : targets) {
                if (entry.id() == null || entry.name() == null) continue;
                adapter.onPlayerBanned(
                        entry.name(),
                        entry.id(),
                        reasonStr,
                        bannedBy
                );
            }
        } catch (Exception e) {
            FabricMod.log("BanCommandMixin error: {}", e.getMessage());
        }
    }
}
