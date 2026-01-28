package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import com.mojang.authlib.GameProfile;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.OpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(OpCommand.class)
public class OpCommandMixin {
    @Inject(method = "opPlayers", at = @At("HEAD"))
    private static void onOp(CommandSourceStack source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (GameProfile profile : targets) {
                if (profile.id() == null || profile.name() == null) continue;
                adapter.onPlayerOp(profile.id(), profile.name());
            }
        } catch (Exception e) {
            FabricMod.log("OpCommandMixin error: {}", e.getMessage());
        }
    }
}
