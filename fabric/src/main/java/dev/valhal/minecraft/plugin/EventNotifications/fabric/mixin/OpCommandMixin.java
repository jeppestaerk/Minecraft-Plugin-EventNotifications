package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.OpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(OpCommand.class)
public class OpCommandMixin {
    @Inject(method = "op", at = @At("HEAD"))
    private static void onOp(ServerCommandSource source, Collection<PlayerConfigEntry> targets, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            for (PlayerConfigEntry entry : targets) {
                if (entry.id() == null || entry.name() == null) continue;
                adapter.onPlayerOp(entry.id(), entry.name());
            }
        } catch (Exception e) {
            FabricMod.log("OpCommandMixin error: {}", e.getMessage());
        }
    }
}
