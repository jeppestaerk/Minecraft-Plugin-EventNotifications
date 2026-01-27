package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import net.minecraft.server.command.KickCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(KickCommand.class)
public class KickCommandMixin {
    @Inject(method = "execute", at = @At("HEAD"))
    private static void onKick(ServerCommandSource source, Collection<ServerPlayerEntity> targets, Text reason, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            String reasonStr = reason != null ? reason.getString() : "Kicked by operator";

            for (ServerPlayerEntity player : targets) {
                adapter.onPlayerKicked(
                        player.getUuid(),
                        player.getName().getString(),
                        reasonStr
                );
            }
        } catch (Exception e) {
            // Don't let notification errors break the command
        }
    }
}
