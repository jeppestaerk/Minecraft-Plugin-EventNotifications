package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import com.mojang.authlib.GameProfile;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PardonCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(PardonCommand.class)
public class PardonCommandMixin {
    @Inject(method = "pardonPlayers", at = @At("HEAD"))
    private static void onPardon(CommandSourceStack source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (GameProfile profile : targets) {
                if (profile.id() == null || profile.name() == null) continue;
                adapter.onPlayerPardoned(profile.id(), profile.name());
            }
        } catch (Exception e) {
            FabricMod.log("PardonCommandMixin error: {}", e.getMessage());
        }
    }
}
