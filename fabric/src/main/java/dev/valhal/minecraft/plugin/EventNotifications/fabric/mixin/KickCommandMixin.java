package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(KickCommand.class)
public class KickCommandMixin {
    @Inject(method = "kickPlayers", at = @At("HEAD"))
    private static void onKick(CommandSourceStack source, Collection<ServerPlayer> targets, Component reason, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            String reasonStr = reason != null ? reason.getString() : "Kicked by operator";

            for (ServerPlayer player : targets) {
                adapter.onPlayerKicked(
                        player.getUUID(),
                        player.getName().getString(),
                        reasonStr
                );
            }
        } catch (Exception e) {
            // Don't let notification errors break the command
        }
    }
}
