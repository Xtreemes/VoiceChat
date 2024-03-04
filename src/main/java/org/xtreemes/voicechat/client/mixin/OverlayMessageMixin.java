package org.xtreemes.voicechat.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public class OverlayMessageMixin {

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onOverlayMessage(GameMessageS2CPacket packet, CallbackInfo ci){
        System.out.println("Received! " + packet.toString());
    }
}
