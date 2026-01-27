package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.PardonCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(PardonCommand.class)
public class PardonCommandMixin {
    @Inject(method = "pardon", at = @At("HEAD"))
    private static void onPardon(ServerCommandSource source, Collection<PlayerConfigEntry> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (PlayerConfigEntry entry : targets) {
                if (entry.id() == null || entry.name() == null) continue;
                adapter.onPlayerPardoned(entry.id(), entry.name());
            }
        } catch (Exception e) {
            FabricMod.log("PardonCommandMixin error: {}", e.getMessage());
        }
    }
}
