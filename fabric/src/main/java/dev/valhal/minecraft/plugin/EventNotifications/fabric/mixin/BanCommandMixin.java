package dev.valhal.minecraft.plugin.EventNotifications.fabric.mixin;

import com.mojang.authlib.GameProfile;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricEventAdapter;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.FabricMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.BanPlayerCommands;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(BanPlayerCommands.class)
public class BanCommandMixin {
    @Inject(method = "banPlayers", at = @At("HEAD"))
    private static void onBan(CommandSourceStack source, Collection<GameProfile> targets, @Nullable Component reason, CallbackInfoReturnable<Integer> cir) {
        try {
            FabricEventAdapter adapter = FabricEventAdapter.getInstance();
            if (adapter == null) return;

            String bannedBy = source.getTextName();
            String reasonStr = reason != null ? reason.getString() : "No reason given";

            for (GameProfile profile : targets) {
                if (profile.id() == null || profile.name() == null) continue;
                adapter.onPlayerBanned(
                        profile.name(),
                        profile.id(),
                        reasonStr,
                        bannedBy
                );
            }
        } catch (Exception e) {
            FabricMod.log("BanCommandMixin error: {}", e.getMessage());
        }
    }
}
