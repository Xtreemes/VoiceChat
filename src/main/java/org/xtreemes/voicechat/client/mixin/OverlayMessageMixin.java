package org.xtreemes.voicechat.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xtreemes.voicechat.client.VoiceChatManager;

@Mixin(ClientPlayNetworkHandler.class)
public class OverlayMessageMixin {

    @Inject(method = "onOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onOverlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci){
        String message = packet.getMessage().getString();
        if(message.startsWith("VOICE_CHAT:") && !message.equals("VOICE_CHAT:")){
            String data = message.substring(11);

            String[] split_data = data.split(",");
            for(String s : split_data){
                String[] sub_data = s.split(":");
                if(sub_data.length == 2) {
                    VoiceChatManager.setUser(sub_data[0], sub_data[1]);
                } else {
                    System.out.println("Incorrect <name>:<volume> packet!");
                }
            }

            ci.cancel();
        }
    }
}
