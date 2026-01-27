package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {
    @Inject(method = "executeAdd", at = @At("HEAD"))
    private static void onWhitelistAdd(ServerCommandSource source, Collection<PlayerConfigEntry> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (PlayerConfigEntry entry : targets) {
                if (entry.id() == null || entry.name() == null) continue;
                adapter.onPlayerWhitelisted(entry.id(), entry.name());
            }
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin add error: {}", e.getMessage());
        }
    }

    @Inject(method = "executeRemove", at = @At("HEAD"))
    private static void onWhitelistRemove(ServerCommandSource source, Collection<PlayerConfigEntry> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (PlayerConfigEntry entry : targets) {
                if (entry.id() == null || entry.name() == null) continue;
                adapter.onPlayerUnwhitelisted(entry.id(), entry.name());
            }
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin remove error: {}", e.getMessage());
        }
    }

    @Inject(method = "executeOn", at = @At("HEAD"))
    private static void onWhitelistOn(ServerCommandSource source, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;
            adapter.onWhitelistOn();
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin on error: {}", e.getMessage());
        }
    }

    @Inject(method = "executeOff", at = @At("HEAD"))
    private static void onWhitelistOff(ServerCommandSource source, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;
            adapter.onWhitelistOff();
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin off error: {}", e.getMessage());
        }
    }
}
