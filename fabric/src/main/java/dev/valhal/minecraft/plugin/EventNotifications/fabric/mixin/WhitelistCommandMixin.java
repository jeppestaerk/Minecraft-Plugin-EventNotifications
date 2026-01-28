package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import com.mojang.authlib.GameProfile;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WhitelistCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {
    @Inject(method = "addPlayers", at = @At("HEAD"))
    private static void onWhitelistAdd(CommandSourceStack source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (GameProfile profile : targets) {
                if (profile.id() == null || profile.name() == null) continue;
                adapter.onPlayerWhitelisted(profile.id(), profile.name());
            }
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin add error: {}", e.getMessage());
        }
    }

    @Inject(method = "removePlayers", at = @At("HEAD"))
    private static void onWhitelistRemove(CommandSourceStack source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (GameProfile profile : targets) {
                if (profile.id() == null || profile.name() == null) continue;
                adapter.onPlayerUnwhitelisted(profile.id(), profile.name());
            }
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin remove error: {}", e.getMessage());
        }
    }

    @Inject(method = "enableWhitelist", at = @At("HEAD"))
    private static void onWhitelistOn(CommandSourceStack source, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;
            adapter.onWhitelistOn();
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin on error: {}", e.getMessage());
        }
    }

    @Inject(method = "disableWhitelist", at = @At("HEAD"))
    private static void onWhitelistOff(CommandSourceStack source, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;
            adapter.onWhitelistOff();
        } catch (Exception e) {
            FabricMod.log("WhitelistCommandMixin off error: {}", e.getMessage());
        }
    }
}
